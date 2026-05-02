package community.rtsp.routes

import community.rtsp.auth.AuthRepository
import community.rtsp.auth.UserSession
import community.rtsp.config.AppConfig
import community.rtsp.stream.StreamRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun Route.files(
    config: AppConfig,
    streamRepository: StreamRepository,
) {
    get("/api/files/{streamId}") {
        val userId = call.principal<UserSession>()!!.userId
        val streamId = call.parameters["streamId"]?.toLongOrNull()

        if (streamId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val stream = streamRepository.getStreamById(streamId, userId)
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
                    val fullPath = line.toKString().trim()
                    if (fullPath.isNotEmpty()) {
                        files.add(
                            fullPath.removePrefix(config.dataDir)
                                .removePrefix("/${stream.owner_id}/")
                        )
                    }
                }
            }
            pclose(fp)
        }
        call.respond(files)
    }
}
