package community.rtsp.routes

import community.rtsp.routes.util.streamFileToChannel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun Route.frontend(staticDir: String) {
    get("/{path...}") {
        val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
        var file = if (path.isEmpty()) "$staticDir/index.html" else "$staticDir/$path"
        
        // If file not found, serve index.html (SPA routing)
        if (access(file, F_OK) != 0) {
            file = "$staticDir/index.html"
        }

        if (access(file, F_OK) != 0) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        val contentType = when {
            file.endsWith(".html") -> ContentType.Text.Html
            file.endsWith(".js") -> ContentType.parse("application/javascript")
            file.endsWith(".css") -> ContentType.Text.CSS
            file.endsWith(".png") -> ContentType.Image.PNG
            file.endsWith(".jpg") || file.endsWith(".jpeg") -> ContentType.Image.JPEG
            file.endsWith(".svg") -> ContentType.Image.SVG
            file.endsWith(".json") -> ContentType.Application.Json
            file.endsWith(".ico") -> ContentType.parse("image/x-icon")
            else -> ContentType.Application.OctetStream
        }

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
