package community.rtsp.backup

import community.rtsp.config.AppConfig
import community.rtsp.config.StreamConfig
import kotlinx.coroutines.*
import platform.posix.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class BackupService(private val config: AppConfig) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val dataDir = getenv("DATA_DIR")?.toKString() ?: "/data"
    private val recorders = mutableMapOf<String, Job>()

    fun start() {
        config.streams.forEach { streamConfig ->
            recorders[streamConfig.alias] = scope.launch {
                runRecorder(streamConfig)
            }
        }
    }

    private suspend fun CoroutineScope.runRecorder(stream: StreamConfig) {
        val segmentTime = config.properties.segmentTime
        while (isActive) {
            val pid = startFfmpeg(stream)
            if (pid > 0) {
                try {
                    val finishedGracefully = waitWithTimeout(pid, segmentTime)
                    if (!finishedGracefully) {
                        stopFfmpeg(pid)
                    }
                } catch (e: CancellationException) {
                    stopFfmpeg(pid)
                    throw e
                }
            } else {
                delay(5000)
            }
        }
    }

    private fun startFfmpeg(stream: StreamConfig): Int {
        val outDir = getOutputDir(stream.directory)
        system("mkdir -p $outDir")
        
        val timestamp = getTimestamp()
        val outFile = "$outDir/$timestamp.mkv"
        
        val args = listOf(
            "ffmpeg", "-hide_banner", "-y",
            "-loglevel", "error",
            "-rtsp_transport", "tcp",
            "-use_wallclock_as_timestamps", "1",
            "-i", stream.url.trim().removeSuffix("?"),
            "-vcodec", "copy",
            "-acodec", "copy",
            "-strftime", "1",
            "-metadata", "title=Record:${stream.directory}",
            outFile
        )

        val pid = fork()
        if (pid == 0) {
            memScoped {
                val argv = allocArray<CPointerVar<ByteVar>>(args.size + 1)
                args.forEachIndexed { index, arg ->
                    argv[index] = arg.cstr.getPointer(this)
                }
                argv[args.size] = null
                execvp(args[0], argv)
                _exit(1)
            }
        }
        return pid
    }

    private suspend fun CoroutineScope.waitWithTimeout(pid: Int, timeoutSeconds: Int): Boolean {
        val startTime = time(null)
        while (time(null) - startTime < timeoutSeconds) {
            if (!isActive) return false 
            val finished = memScoped {
                val status = alloc<IntVar>()
                val res = waitpid(pid, status.ptr, WNOHANG)
                if (res == pid) true
                else if (res == -1 && errno == ECHILD) true
                else false
            }
            if (finished) return true
            delay(1000)
        }
        return false 
    }

    private fun stopFfmpeg(pid: Int) {
        kill(pid, SIGTERM)
        var reaped = false
        memScoped {
            val status = alloc<IntVar>()
            for (i in 1..10) {
                val res = waitpid(pid, status.ptr, WNOHANG)
                if (res == pid || (res == -1 && errno == ECHILD)) {
                    reaped = true
                    break
                }
                sleep(1u)
            }
        }
        if (!reaped) {
            kill(pid, SIGKILL)
            memScoped {
                val status = alloc<IntVar>()
                waitpid(pid, status.ptr, 0)
            }
        }
    }

    private fun getOutputDir(dirName: String): String = memScoped {
        val now = alloc<time_tVar>().apply { value = time(null) }
        val timeInfo = alloc<tm>()
        localtime_r(now.ptr, timeInfo.ptr)
        val year = (timeInfo.tm_year + 1900).toString()
        val month = (timeInfo.tm_mon + 1).toString().padStart(2, '0')
        val day = timeInfo.tm_mday.toString().padStart(2, '0')
        "$dataDir/$dirName/$year/$month/$day"
    }

    private fun getTimestamp(): String = memScoped {
        val now = alloc<time_tVar>().apply { value = time(null) }
        val timeInfo = alloc<tm>()
        localtime_r(now.ptr, timeInfo.ptr)
        val hour = timeInfo.tm_hour.toString().padStart(2, '0')
        val min = timeInfo.tm_min.toString().padStart(2, '0')
        val sec = timeInfo.tm_sec.toString().padStart(2, '0')
        "$hour-$min-$sec"
    }

    fun stop() {
        scope.cancel()
    }
    
    fun isRecording(): Boolean {
        return recorders.values.any { it.isActive }
    }
}
