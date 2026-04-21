package community.rtsp.routes

import community.rtsp.config.AppConfig
import community.rtsp.routes.util.streamFileToChannel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fopen
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
fun Route.live(config: AppConfig) {
    get("/api/live/{path...}") {
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

        val contentType = when {
            path.endsWith(".m3u8") -> ContentType.parse("application/vnd.apple.mpegurl")
            path.endsWith(".ts") -> ContentType.parse("video/MP2T")
            else -> ContentType.Application.OctetStream
        }

        call.response.header(HttpHeaders.CacheControl, "no-store, no-cache, must-revalidate")

        val fd = fopen(file, "rb")
        if (fd == null) {
            call.respond(HttpStatusCode.InternalServerError)
            return@get
        }

        call.respondBytesWriter(contentType = contentType) {
            streamFileToChannel(fd)
        }
    }
}