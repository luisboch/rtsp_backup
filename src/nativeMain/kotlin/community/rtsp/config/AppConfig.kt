package community.rtsp.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    val autoClean: AutoClean,
    val auth: AuthConfig = AuthConfig()
)

@Serializable
data class AuthConfig(
    val user: String = "admin",
    val pass: String = "admin"
)

@Serializable
data class AutoClean(
    val enabled: Boolean,
    @SerialName("keep_days")
    val keepDays: Int
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class StreamConfig(
    val id: String = Uuid.random().toString(),
    val alias: String,
    val url: String,
    val directory: String
)
