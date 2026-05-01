package community.rtsp.config

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
@Serializable
data class AppConfig  constructor(
    val properties: Properties,
    val dataDir: String = getenv("DATA_DIR")?.toKString() ?: "/data"
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
