package community.rtsp

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import community.rtsp.auth.AuthRepository
import community.rtsp.auth.SessionCleanupService
import community.rtsp.auth.SessionService
import community.rtsp.auth.UserSession
import community.rtsp.auth.authRoutes
import community.rtsp.config.ConfigLoader
import community.rtsp.db.Database
import community.rtsp.routes.live
import community.rtsp.routes.streamRoutes
import community.rtsp.routes.video
import community.rtsp.stream.CleanService
import community.rtsp.stream.StreamBackupService
import community.rtsp.system.AppStatus
import community.rtsp.system.FfmpegCliService
import community.rtsp.system.SystemStatsService
import community.rtsp.util.GenerateRandomService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.utils.io.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Serializable
data class AddStreamRequest(
    val alias: String,
    val rtspUrl: String
)

@Serializable
data class ShareStreamRequest(
    val username: String
)

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val host = getenv("HOST")?.toKString() ?: "0.0.0.0"
    val port = getenv("PORT")?.toKString()?.toIntOrNull() ?: 8080

    embeddedServer(CIO, host = host, port = port) {
        module()
    }.start(wait = true)
}

@OptIn(ExperimentalForeignApi::class)
fun Application.module() {
    val config = ConfigLoader.load()

    val driver: SqlDriver = NativeSqliteDriver(
        configuration = DatabaseConfiguration(
            name = "database.db",
            version = Database.Schema.version.toInt(),
            create = { connection ->
                wrapConnection(connection) { Database.Schema.create(it) }
            },
            upgrade = { connection, oldVersion, newVersion ->
                wrapConnection(connection) { Database.Schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) }
            },
            extendedConfig = DatabaseConfiguration.Extended(
                basePath = config.dataDir
            )
        )
    )
    val database = Database(driver)

    val authRepository = AuthRepository(
        database,
        community.rtsp.auth.PasswordHasher()
    )

    val sessionService = SessionService(database.sessionQueries)
    val sessionCleanupService = SessionCleanupService(sessionService)
    val randomService = GenerateRandomService()

    val systemStatsService = SystemStatsService()
    val ffmpegCliService = FfmpegCliService()
    val backupService = StreamBackupService(config, authRepository)
    val cleanService = CleanService(config, authRepository)

    backupService.start()
    cleanService.start()
    sessionCleanupService.start()

    environment.monitor.subscribe(ApplicationStopping) {
        backupService.stop()
        cleanService.stop()
        sessionCleanupService.stop()
    }

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Put)
    }

    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }

    install(Sessions) {
        cookie<UserSession>("session_id") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = false // Set to false for development (no HTTPS)
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session ->
                if (sessionService.getUserIdByToken(session.token) != null) {
                    session
                } else {
                    null
                }
            }
        }
    }

    routing {
        // Public routes
        get("/health") {
            call.respondText("ok")
        }

        authRoutes(authRepository, sessionService, randomService)

        // Protected routes
        authenticate("auth-session") {
            streamRoutes(authRepository, randomService, backupService)

            get("/api/config") {
                call.respond(config)
            }

            get("/api/status") {
                val stats = systemStatsService.collect()
                call.respond(
                    AppStatus(
                        recording = backupService.isRecording(),
                        streamsConfigured = authRepository.getAllStreams().size,
                        ffmpegAvailable = ffmpegCliService.isAvailable(),
                        diskUsedBytes = stats.diskUsedBytes,
                        diskTotalBytes = stats.diskTotalBytes,
                        timestamp = time(null) * 1000
                    )
                )
            }


            get("/api/files/{alias}") {
                val session = call.principal<UserSession>()
                val userId = session?.userId
                val alias = call.parameters["alias"]

                if (userId == null || alias == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val stream = authRepository.getStreamByAlias(alias, userId)

                if (stream == null) {
                    call.respond(HttpStatusCode.NoContent)
                    return@get
                }

                val dirPath = "${config.dataDir}/${stream.owner_id}/${stream.directory}"

                val files = mutableListOf<String>()
                val cmd = "find '$dirPath/backup' -type f -name '*.mp4' | sort -r"
                val fp = popen(cmd, "r")
                if (fp != null) {
                    memScoped {
                        val line = allocArray<ByteVar>(1024)
                        while (fgets(line, 1024, fp) != null) {
                            files.add(
                                line.toKString().trim().removePrefix(config.dataDir)
                                    .removePrefix("/${stream.owner_id}/")
                            )
                        }
                    }
                    pclose(fp)
                }
                call.respond(files)
            }

            get("/api/stats/sse") {
                call.response.headers.append("Cache-Control", "no-cache")
                call.response.headers.append("Connection", "keep-alive")
                call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                    while (true) {
                        val stats = systemStatsService.collect()
                        val payload = Json.encodeToString(stats)
                        writeStringUtf8("event: stats\ndata: $payload\n\n")
                        flush()
                        delay(call.request.queryParameters["interval"]?.toLongOrNull()?.milliseconds ?: 1.seconds)
                    }
                }
            }

            live(config)
            video(config)
        }
    }
}
