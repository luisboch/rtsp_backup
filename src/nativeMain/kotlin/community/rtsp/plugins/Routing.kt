package community.rtsp.plugins

import community.rtsp.auth.*
import community.rtsp.config.AppConfig
import community.rtsp.routes.*
import community.rtsp.stream.*
import community.rtsp.system.*
import community.rtsp.util.GenerateRandomService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
fun Application.configureRouting(
    config: AppConfig,
    authRepository: AuthRepository,
    streamRepository: StreamRepository,
    sessionService: SessionService,
    randomService: GenerateRandomService,
    backupService: StreamBackupService,
    systemStatsService: SystemStatsService,
    ffmpegCliService: FfmpegCliService
) {
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Put)
    }

    routing {
        get("/health") {
            call.respondText("ok")
        }

        route("/api/p") {
            authRoutes(authRepository, sessionService, randomService)
        }

        authenticate("auth-session") {
            streamRoutes(authRepository, streamRepository, randomService, backupService)

            get("/api/config") {
                call.respond(config)
            }

            get("/api/status") {
                val stats = systemStatsService.collect()
                call.respond(
                    AppStatus(
                        recording = backupService.isRecording(),
                        streamsConfigured = streamRepository.getAllStreams().size,
                        ffmpegAvailable = ffmpegCliService.isAvailable(),
                        diskUsedBytes = stats.diskUsedBytes,
                        diskTotalBytes = stats.diskTotalBytes,
                        timestamp = time(null) * 1000
                    )
                )
            }


            get("/api/stats/sse") {
                call.response.headers.append("Cache-Control", "no-cache")
                call.response.headers.append("Connection", "keep-alive")
                call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                    try {
                        while (true) {
                            val stats = systemStatsService.collect()
                            val payload = Json.encodeToString(stats)
                            writeStringUtf8("event: stats\ndata: $payload\n\n")
                            flush()
                            delay(call.request.queryParameters["interval"]?.toLongOrNull()?.milliseconds ?: 1.seconds)
                        }
                    } catch (e: Exception) {
                        println("SSE error: ${e.message}")
                    }
                }
            }

            files(config, streamRepository)

            live(config, streamRepository)
            video(config, streamRepository)
        }
    }
}