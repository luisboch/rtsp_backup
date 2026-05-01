package community.rtsp.system

import kotlinx.cinterop.*
import platform.posix.*
import platform.linux.*
import kotlin.math.round

@OptIn(ExperimentalForeignApi::class)
class SystemStatsService {
    private var previousTotalTicks: Long = 0
    private var previousIdleTicks: Long = 0

    fun collect(): SystemStats {
        val memory = readMemory()
        val disk = readDisk()
        val cpuLoad = round(readCpuLoadPercent() * 100.0) / 100.0

        return SystemStats(
            cpuLoadPercent = cpuLoad,
            memoryUsedBytes = memory.first,
            memoryTotalBytes = memory.second,
            diskUsedBytes = disk.first,
            diskTotalBytes = disk.second,
            timestamp = time(null) * 1000
        )
    }

    private fun readMemory(): Pair<Long, Long> {
        val lines = readLines("/proc/meminfo")
        val totalKb = lines.firstOrNull { it.startsWith("MemTotal:") }?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull() ?: 0L
        val availableKb = lines.firstOrNull { it.startsWith("MemAvailable:") }?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull() ?: 0L
        val total = totalKb * 1024
        val used = (totalKb - availableKb).coerceAtLeast(0) * 1024
        return used to total
    }

    private fun readDisk(): Pair<Long, Long> = memScoped {
        val stats = alloc<statfs>()
        if (statfs("/", stats.ptr) != 0) {
            return@memScoped 0L to 0L
        }

        val total = stats.f_blocks.toLong() * stats.f_bsize
        val available = stats.f_bavail.toLong() * stats.f_bsize
        (total - available) to total
    }

    private fun readCpuLoadPercent(): Double {
        val cpuLine = readLines("/proc/stat").firstOrNull { it.startsWith("cpu ") } ?: return 0.0
        val parts = cpuLine.trim().split(Regex("\\s+"))
        if (parts.size < 5) return 0.0

        val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
        if (values.size < 4) return 0.0

        val idle = values[3] + values.getOrElse(4) { 0L }
        val total = values.sum()

        if (previousTotalTicks == 0L || previousIdleTicks == 0L) {
            previousTotalTicks = total
            previousIdleTicks = idle
            return 0.0
        }

        val totalDelta = total - previousTotalTicks
        val idleDelta = idle - previousIdleTicks
        previousTotalTicks = total
        previousIdleTicks = idle
        if (totalDelta <= 0) return 0.0

        return ((totalDelta - idleDelta).toDouble() / totalDelta.toDouble()).coerceIn(0.0, 1.0) * 100.0
    }

    private fun readLines(path: String): List<String> {
        val content = readText(path)
        if (content.isEmpty()) return emptyList()
        return content.lineSequence().toList()
    }

    private fun readText(path: String): String {
        val file = fopen(path, "rb") ?: return ""
        return try {
            readAll(file)
        } finally {
            fclose(file)
        }
    }

    private fun readAll(file: CPointer<FILE>): String {
        val chunks = StringBuilder()
        memScoped {
            val buffer = allocArray<ByteVar>(4096)
            while (true) {
                val read = fread(buffer, 1u, 4096u, file).toInt()
                if (read <= 0) break
                chunks.append(buffer.readBytes(read).decodeToString())
            }
        }
        return chunks.toString()
    }
}
