package community.rtsp.dto

import community.rtsp.db.GetStreamsForUser
import community.rtsp.db.Stream
import kotlinx.serialization.Serializable

@Serializable
data class StreamDto(
    val id: Long,
    val ownerId: Long,
    val alias: String,
    val rtspUrl: String,
    val directory: String,
    val isFavorite: Boolean = false,
    val isOwner: Boolean = false,
) {
    companion object {
        fun GetStreamsForUser.toDto(currentUserId: Long): StreamDto = StreamDto(
            id = id,
            ownerId = owner_id,
            alias = alias,
            rtspUrl = rtsp_url,
            directory = directory,
            isFavorite = is_favorite == 1L,
            isOwner = owner_id == currentUserId
        )

        fun Stream.toDto(currentUserId: Long, isFavorite: Boolean = false): StreamDto = StreamDto(
            id = id,
            ownerId = owner_id,
            alias = alias,
            rtspUrl = rtsp_url,
            directory = directory,
            isFavorite = isFavorite,
            isOwner = owner_id == currentUserId
        )
    }
}

@Serializable
data class AddStreamRequest(
    val alias: String,
    val rtspUrl: String
)

@Serializable
data class ShareStreamRequest(
    val username: String
)