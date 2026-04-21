package community.rtsp.routes

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
fun Route.video() {
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
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                path.substringAfterLast("/")
            ).toString()
        )

        val fd = fopen(file, "rb")
        if (fd == null) {
            call.respond(HttpStatusCode.InternalServerError)
            return@get
        }

        call.respondBytesWriter(contentType = ContentType.parse("video/mp4")) {
            streamFileToChannel(fd)
        }
    }
}
