package community.rtsp

import community.rtsp.backup.BackupService
import community.rtsp.config.ConfigLoader
import community.rtsp.system.AppStatus
import community.rtsp.system.FfmpegCliService
import community.rtsp.system.SystemStatsService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.posix.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
    val backupService = BackupService(config)

    backupService.start()

    environment.monitor.subscribe(ApplicationStopping) {
        backupService.stop()
    }

    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
    }

    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }

    routing {
        get("/health") {
            call.respondText("ok")
        }

        get("/api/config") {
            call.respond(config)
        }

        get("/api/status") {
            call.respond(
                AppStatus(
                    recording = backupService.isRecording(),
                    streamsConfigured = config.streams.size,
                    ffmpegAvailable = ffmpegCliService.isAvailable(),
                    timestamp = time(null).toLong() * 1000
                )
            )
        }

        get("/api/streams") {
            call.respond(config.streams)
        }

        get("/api/proxy/{alias}") {
            val alias = call.parameters["alias"]
            val stream = config.streams.find { it.alias == alias }
            if (stream == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.response.headers.append("Cache-Control", "no-cache, no-store, must-revalidate")
            call.response.headers.append("Pragma", "no-cache")
            call.response.headers.append("Expires", "0")
            call.response.headers.append("Connection", "close")

            call.respondBytesWriter(contentType = ContentType.parse("multipart/x-mixed-replace; boundary=frame")) {

                val args = listOf(
                    "ffmpeg",
                    "-hide_banner", "-loglevel", "error",
                    "-rtsp_transport", "tcp",
                    "-i", stream.url,
                    "-f", "mpjpeg",
                    "-q", "5",
                    "-r", "5",
                    "pipe:1"
                )

                // Escape each argument for shell
                val escapedCmd = args.joinToString(" ") { arg ->
                    val cleanArg = if (arg.startsWith("rtsp://")) arg.trim().removeSuffix("?") else arg
                    "'" + cleanArg.replace("'", "'\\''") + "'"
                }

                println("Executing command:")
                println(escapedCmd)

                val fp = popen(escapedCmd, "r")
                if (fp == null) return@respondBytesWriter

                try {
                    val buffer = ByteArray(16384)
                    println("Reading stream")
                    while (true) {
                        val read = memScoped {
                            val nativeBuffer = allocArray<ByteVar>(buffer.size)
                            val r = fread(nativeBuffer, 1u, buffer.size.toULong(), fp).toInt()
                            if (r > 0) {
                                for (i in 0 until r) buffer[i] = nativeBuffer[i]
                            }
                            r
                        }
                        if (read <= 0) break
                        writeFully(buffer, 0, read)
                        flush()
                    }
                } finally {
                    println("Read finished")
                    pclose(fp)
                }
            }
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
            val cmd = "find '$dirPath' -type f -name '*.mkv' | sort -r"
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

        get("/api/video/{path...}") {
            val path = call.parameters.getAll("path")?.joinToString("/")
            if (path == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val dataDir = getenv("DATA_DIR")?.toKString() ?: "/data"
            val file = "$dataDir/$path"

            if (access(file, F_OK) != 0) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, path.substringAfterLast("/")).toString()
            )
            
            val fd = fopen(file, "rb")
            if (fd == null) {
                call.respond(HttpStatusCode.InternalServerError)
                return@get
            }

            call.respondBytesWriter(contentType = ContentType.parse("video/x-matroska")) {
                try {
                    val buffer = ByteArray(65536)
                    while (true) {
                        val read = memScoped {
                            val nativeBuffer = allocArray<ByteVar>(buffer.size)
                            val r = fread(nativeBuffer, 1u, buffer.size.toULong(), fd).toInt()
                            if (r > 0) {
                                for (i in 0 until r) buffer[i] = nativeBuffer[i]
                            }
                            r
                        }
                        if (read <= 0) break
                        writeFully(buffer, 0, read)
                        flush()
                    }
                } finally {
                    fclose(fd)
                }
            }
        }

        get("/api/stats/sse") {
            call.response.headers.append("Cache-Control", "no-cache")
            call.response.headers.append("Connection", "keep-alive")
            call.respondBytesWriter(contentType = io.ktor.http.ContentType.Text.EventStream) {
                while (true) {
                    val stats = systemStatsService.collect()
                    val payload = Json.encodeToString(stats)
                    "event: stats\n".toByteArray().forEach {
                        writeByte(it)
                    }

                    "data: $payload\n\n".toByteArray().forEach {
                        writeByte(it)
                    }

                    flush()
                    delay(call.request.queryParameters["interval"]?.toLongOrNull()?.milliseconds ?: 1.seconds)
                }
            }
        }
    }
}
