package community.rtsp.system

import kotlinx.serialization.Serializable

@Serializable
data class SystemStats(
    val cpuLoadPercent: Double,
    val memoryUsedBytes: Long,
    val memoryTotalBytes: Long,
    val diskUsedBytes: Long,
    val diskTotalBytes: Long,
    val timestamp: Long
)
