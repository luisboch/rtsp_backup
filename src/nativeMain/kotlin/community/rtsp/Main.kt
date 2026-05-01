package community.rtsp

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import community.rtsp.auth.AuthRepository
import community.rtsp.auth.SessionService
import community.rtsp.auth.UserSession
import community.rtsp.auth.authRoutes
import community.rtsp.config.ConfigLoader
import community.rtsp.db.Database
import community.rtsp.dto.StreamDto.Companion.toDto
import community.rtsp.routes.live
import community.rtsp.routes.streamProxy
import community.rtsp.routes.video
import community.rtsp.stream.CleanService
import community.rtsp.stream.StreamService
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
import io.ktor.server.request.*
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

    // Database & Auth Initialization
    val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db")
    val database = Database(driver)

    val authRepository = AuthRepository(
        database,
        community.rtsp.auth.PasswordHasher()
    )

    val sessionService = SessionService(database.sessionQueries)
    val randomService = GenerateRandomService()

    val systemStatsService = SystemStatsService()
    val ffmpegCliService = FfmpegCliService()
    val backupService = StreamService(config, authRepository)
    val cleanService = CleanService(config, authRepository)

    backupService.start()
    cleanService.start()

    environment.monitor.subscribe(ApplicationStopping) {
        backupService.stop()
        cleanService.stop()
    }

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
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
            streamProxy(authRepository)

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

            get("/api/streams") {
                val session = call.principal<UserSession>()
                val userId = session?.userId
                if (userId != null) {
                    val streams = authRepository.getStreamsForUser(userId)
                        .executeAsList().map { stream -> stream.toDto() }
                    call.respond(streams)
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }

            post("/api/streams") {
                val session = call.principal<UserSession>()
                val userId = session?.userId
                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = try {
                    call.receive<AddStreamRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid request body"))
                    return@post
                }

                if (request.alias.isBlank() || request.rtspUrl.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("message" to "All fields are required"))
                    return@post
                }

                try {
                    val directory = randomService.generate(16)
                    authRepository.addStream(
                        ownerId = userId,
                        alias = request.alias,
                        url = request.rtspUrl,
                        dir = directory
                    )
                    
                    val newStream = authRepository.getStreamByAlias(request.alias)
                    if (newStream != null) {
                        backupService.startStreamRecording(newStream)
                        call.respond(HttpStatusCode.Created, newStream.toDto())
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to retrieve created stream"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.Conflict, mapOf("message" to "Stream alias already exists or database error"))
                }
            }

            get("/api/files/{alias}") {
                val session = call.principal<UserSession>()
                val userId = session?.userId
                val alias = call.parameters["alias"]

                if (userId == null || alias == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }

                val stream = authRepository.getStreamByAlias(alias)

                if (stream == null) {
                    call.respond(HttpStatusCode.NoContent)
                    return@get
                }

                // Check if user has access (owner or shared)
                if (stream.owner_id != userId) {
                    // This is a simplified check.
                    call.respond(HttpStatusCode.Forbidden)
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
                            files.add(line.toKString().trim().removePrefix(config.dataDir).removePrefix("/"))
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
