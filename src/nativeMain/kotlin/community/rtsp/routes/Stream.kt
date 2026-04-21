package community.rtsp.routes

import community.rtsp.config.AppConfig
import community.rtsp.routes.util.bufferedRead
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.FILE
import platform.posix.pclose
import platform.posix.popen


@OptIn(ExperimentalForeignApi::class)
fun Route.streamProxy(config: AppConfig) {
    get("/api/stream/proxy/{uuid}") {
        val uuid = call.parameters["uuid"]
        val stream = config.streams.find { it.id == uuid }

        if (stream == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.response.headers.append(
            HttpHeaders.CacheControl,
            "no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0"
        )
        call.response.headers.append(HttpHeaders.Connection, "keep-alive")
        call.response.headers.append(
            HttpHeaders.ContentType,
            "multipart/x-mixed-replace; boundary=ffmpeg"
        )

        // 2. RespondBytesWriter sem formatação extra (sem SSE)
        call.respondBytesWriter {
            var ffmpegProcess: CPointer<FILE>? = null
            println("Starting Direct MJPEG stream for: $uuid")

            try {
                val args = listOf(
                    "ffmpeg",
                    "-hide_banner", "-loglevel", "error",
                    "-rtsp_transport", "tcp",
                    "-i", stream.url,
                    "-an",
                    "-vcodec", "mjpeg",
                    "-pix_fmt", "yuvj420p",
                    "-f", "mpjpeg",
                    "-q", "5",
                    "-r", "5",
                    "pipe:1"
                )

                val escapedCmd = args.joinToString(" ") { arg ->
                    val cleanArg = if (arg.startsWith("rtsp://")) arg.trim().removeSuffix("?") else arg
                    "'" + cleanArg.replace("'", "'\\''") + "'"
                }

                println("Executing command: $escapedCmd")

                ffmpegProcess = popen(escapedCmd, "r")
                if (ffmpegProcess == null) {
                    println("Failed to start ffmpeg process for: $uuid")
                    return@respondBytesWriter
                }

                val buffer = ByteArray(16384)
                bufferedRead(buffer, ffmpegProcess)

            } finally {
                ffmpegProcess?.let { pclose(it) }
                println("Closed Direct MJPEG stream for: $uuid")
            }
        }
    }
}
