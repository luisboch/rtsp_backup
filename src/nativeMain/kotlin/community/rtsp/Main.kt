package community.rtsp

import community.rtsp.config.AppConfig
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import community.rtsp.stream.StreamService
import community.rtsp.stream.CleanService
import community.rtsp.config.ConfigLoader
import community.rtsp.routes.live
import community.rtsp.routes.streamProxy
import community.rtsp.routes.video
import community.rtsp.system.AppStatus
import community.rtsp.system.FfmpegCliService
import community.rtsp.system.SystemStatsService
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import io.ktor.utils.io.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

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
    val systemStatsService = SystemStatsService()
    val ffmpegCliService = FfmpegCliService()
    val backupService = StreamService(config)
    val cleanService = CleanService(config)

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

    install(Authentication) {
        basic("auth-basic") {
            realm = "Access to the RTSP Backup"
            validate { credentials ->
                if (credentials.name == config.properties.auth.user && credentials.password == config.properties.auth.pass) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

    routing {
        authenticate("auth-basic") {
            streamProxy(config)
            get("/health") {
                call.respondText("ok")
            }

            get("/api/config") {
                call.respond(config)
            }

            get("/api/status") {
                val stats = systemStatsService.collect()
                call.respond(
                    AppStatus(
                        recording = backupService.isRecording(),
                        streamsConfigured = config.streams.size,
                        ffmpegAvailable = ffmpegCliService.isAvailable(),
                        diskUsedBytes = stats.diskUsedBytes,
                        diskTotalBytes = stats.diskTotalBytes,
                        timestamp = time(null).toLong() * 1000
                    )
                )
            }

            get("/api/streams") {
                call.respond(config.streams)
            }

            get("/api/files/{alias}") {
                val alias = call.parameters["alias"]
                val stream = config.streams.find { it.alias == alias }
                if (stream == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                val dataDir = getenv("DATA_DIR")?.toKString() ?: "/data"
                val dirPath = "$dataDir/${stream.directory}"

                val files = mutableListOf<String>()
                val cmd = "find '$dirPath/backup' -type f -name '*.mp4' | sort -r"
                val fp = popen(cmd, "r")
                if (fp != null) {
                    memScoped {
                        val line = allocArray<ByteVar>(1024)
                        while (fgets(line, 1024, fp) != null) {
                            files.add(line.toKString().trim().removePrefix(dataDir).removePrefix("/"))
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
            streamProxy(config)
            video()
        }
    }
}
