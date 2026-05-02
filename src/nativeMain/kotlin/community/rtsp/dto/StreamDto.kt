package community.rtsp.dto

import community.rtsp.db.Stream
import kotlinx.serialization.Serializable

@Serializable
data class StreamDto(
    val id: Long,
    val ownerId: Long,
    val alias: String,
    val rtspUrl: String,
    val directory: String,
) {
    companion object {
        fun Stream.toDto(): StreamDto = StreamDto(
            id = id,
            ownerId = owner_id,
            alias = alias,
            rtspUrl = rtsp_url,
            directory = directory
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