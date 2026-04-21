package community.rtsp.stream

import community.rtsp.config.AppConfig
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalForeignApi::class)
class CleanService(private val config: AppConfig) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val dataDir = getenv("DATA_DIR")?.toKString() ?: "/data"

    fun start() {
        if (!config.properties.autoClean.enabled) {
            println("AutoClean is disabled.")
            return
        }

        println("Starting CleanService with keepDays=${config.properties.autoClean.keepDays}")
        scope.launch {
            while (isActive) {
                try {
                    cleanOldBackups()
                } catch (e: Exception) {
                    println("Error during cleanup: ${e.message}")
                }
                // Run every 12 hours
                delay(12.hours)
            }
        }
    }

    private fun cleanOldBackups() {
        val keepDays = config.properties.autoClean.keepDays
        if (keepDays <= 0) return

        val currentTime = time(null)
        val maxAgeSeconds = keepDays * 24 * 60 * 60

        config.streams.forEach { stream ->
            val backupDir = "$dataDir/${stream.directory}/backup"
            println("Checking for old backups in: $backupDir")
            
            val dir = opendir(backupDir)
            if (dir == null) {
                // Directory might not exist yet if no backups were made
                return@forEach
            }

            try {
                while (true) {
                    val entry = readdir(dir) ?: break
                    val name = entry.pointed.d_name.toKString()
                    if (name == "." || name == "..") continue

                    val filePath = "$backupDir/$name"
                    memScoped {
                        val statBuf = alloc<stat>()
                        if (stat(filePath, statBuf.ptr) == 0) {
                            val fileAgeSeconds = currentTime - statBuf.st_mtim.tv_sec
                            if (fileAgeSeconds > maxAgeSeconds) {
                                println("Deleting old backup: $filePath (age: ${fileAgeSeconds / 3600} hours)")
                                unlink(filePath)
                            }
                        }
                    }
                }
            } finally {
                closedir(dir)
            }
        }
    }

    fun stop() {
        println("Stopping CleanService.")
        scope.cancel()
        println("Stopped CleanService.")
    }
}
