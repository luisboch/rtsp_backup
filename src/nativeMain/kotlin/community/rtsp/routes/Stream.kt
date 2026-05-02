package community.rtsp.routes

import community.rtsp.dto.AddStreamRequest
import community.rtsp.dto.ShareStreamRequest
import community.rtsp.auth.AuthRepository
import community.rtsp.auth.UserSession
import community.rtsp.dto.StreamDto.Companion.toDto
import community.rtsp.routes.util.bufferedRead
import community.rtsp.stream.StreamBackupService
import community.rtsp.system.FfmpegCommandBuilder
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
    get("/api/streams") {
        val userId = call.principal<UserSession>()!!.userId
        val streams = authRepository.getStreamsForUser(userId)
            .executeAsList().map { stream -> stream.toDto() }
        call.respond(streams)
    }

    post("/api/streams") {
        val userId = call.principal<UserSession>()!!.userId

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
        val userId = call.principal<UserSession>()!!.userId

        val streamId = call.parameters["id"]?.toLongOrNull()

        println("DELETE /api/streams/${streamId ?: "null"}")
        if (streamId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid stream ID"))
            return@delete
        }

        val stream = authRepository.getStreamById(streamId, userId)
        if (stream == null) {
            call.respond(HttpStatusCode.NoContent, mapOf("message" to "Stream not found"))
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

    post("/api/streams/{id}/share") {
        val userId = call.principal<UserSession>()!!.userId

        val streamId = call.parameters["id"]?.toLongOrNull()
        if (streamId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid stream ID"))
            return@post
        }

        val request = try {
            call.receive<ShareStreamRequest>()
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Invalid request body"))
            return@post
        }

        // Search for user by login
        val targetUser = authRepository.getUserByUsername(request.username)
        if (targetUser == null) {
            // If not found, return info to front-end
            call.respond(HttpStatusCode.NotFound, mapOf("message" to "User not found"))
            return@post
        }

        try {
            // If found, link stream with shared using stream_share table
            authRepository.shareStream(streamId, targetUser.id)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Stream shared successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.Conflict, mapOf("message" to "Already shared or database error"))
        }
    }
}
