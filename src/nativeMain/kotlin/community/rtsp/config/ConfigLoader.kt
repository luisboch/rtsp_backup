package community.rtsp.config

import kotlinx.cinterop.*
import kotlinx.serialization.json.Json
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
object ConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AppConfig {
        val envPath = getenv("CONFIG_PATH")?.toKString()
        val candidatePaths = listOfNotNull(envPath, "conf/config.json", "conf/config.json.sample")

        val file = candidatePaths
            .firstOrNull(::canRead)
            ?: error("No configuration file found in: $candidatePaths")

        return json.decodeFromString(readText(file))
    }

    private fun canRead(path: String): Boolean {
        val file = fopen(path, "rb") ?: return false
        fclose(file)
        return true
    }

    private fun readText(path: String): String {
        val file = fopen(path, "rb") ?: error("Could not open config file: $path")
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
