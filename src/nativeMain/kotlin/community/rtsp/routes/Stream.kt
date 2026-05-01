package community.rtsp.routes

import community.rtsp.AddStreamRequest
import community.rtsp.auth.AuthRepository
import community.rtsp.auth.UserSession
import community.rtsp.dto.StreamDto.Companion.toDto
import community.rtsp.routes.util.bufferedRead
import community.rtsp.stream.StreamBackupService
import community.rtsp.util.GenerateRandomService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.FILE
import platform.posix.pclose
import platform.posix.popen


@OptIn(ExperimentalForeignApi::class)
fun Route.streamRoutes(
    authRepository: AuthRepository,
    randomService: GenerateRandomService,
    backupService: StreamBackupService,
) {
    get("/api/stream/proxy/{uuid}") {
        val uuid = call.parameters["uuid"]
        val stream = authRepository.getAllStreams().find { it.alias == uuid }

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
                    "-i", stream.rtsp_url,
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

            val newStream = authRepository.getStreamByAlias(request.alias, userId)
            if (newStream != null) {
                backupService.startStreamRecording(newStream)
                call.respond(HttpStatusCode.Created, newStream.toDto())
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("message" to "Failed to retrieve created stream")
                )
            }
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.Conflict,
                mapOf("message" to "Stream alias already exists or database error")
            )
        }
    }

    delete("/api/streams/{id}") {
        val session = call.principal<UserSession>()
        val userId = session?.userId
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return@delete
        }

        val streamId = call.parameters["id"]?.toLongOrNull()
        if (streamId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid stream ID"))
            return@delete
        }

        val stream = authRepository.getStreamById(streamId)
        if (stream == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("message" to "Stream not found"))
            return@delete
        }

        if (stream.owner_id == userId) {
            // Owner: delete from shared and inactivate
            authRepository.deleteAllShares(streamId)
            authRepository.inactivateStream(streamId)
            backupService.stopStreamRecording(stream.alias)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Stream inactivated"))
        } else {
            // Not owner: check if it's shared with this user
            authRepository.unshareStream(streamId, userId)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Stream unshared"))
        }
    }
}
