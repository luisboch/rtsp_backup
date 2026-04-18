package community.rtsp.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val properties: Properties,
    val streams: List<StreamConfig>
)

@Serializable
data class Properties(
    @SerialName("segment_time")
    val segmentTime: Int,
    @SerialName("auto_clean")
    val autoClean: AutoClean
)

@Serializable
data class AutoClean(
    val enabled: Boolean,
    @SerialName("keep_days")
    val keepDays: Int
)

@Serializable
data class StreamConfig(
    val alias: String,
    val url: String,
    val directory: String
)
