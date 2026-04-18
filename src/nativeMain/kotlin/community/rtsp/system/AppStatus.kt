package community.rtsp.system

import kotlinx.serialization.Serializable

@Serializable
data class AppStatus(
    val recording: Boolean,
    val streamsConfigured: Int,
    val ffmpegAvailable: Boolean,
    val timestamp: Long = 0
)
