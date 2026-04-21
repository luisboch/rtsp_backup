package community.rtsp.stream

import community.rtsp.config.AppConfig
import community.rtsp.config.StreamConfig
import kotlinx.coroutines.*
import platform.posix.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class StreamService(private val config: AppConfig) {
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
        while (isActive) {
            val pid = startFfmpeg(stream)
            if (pid > 0) {
                try {
                    // We wait indefinitely while the process is running.
                    // If it dies, we restart it.
                    while (isActive) {
                        val finished = memScoped {
                            val status = alloc<IntVar>()
                            val res = waitpid(pid, status.ptr, WNOHANG)
                            if (res == pid || (res == -1 && errno == ECHILD)) true else false
                        }
                        if (finished) break
                        delay(5000)
                    }
                } catch (e: CancellationException) {
                    kill(pid, SIGTERM)
                    throw e
                }
            } else {
                delay(5000)
            }
        }
    }

    private fun startFfmpeg(stream: StreamConfig): Int {
        val cameraDir = "$dataDir/${stream.directory}"
        val liveDir = "$cameraDir/live"
        val backupDir = "$cameraDir/backup"
        
        system("mkdir -p $liveDir")
        system("mkdir -p $backupDir")
        
        val rtspUrl = stream.url.trim().removeSuffix("?")
        val segmentTime = config.properties.segmentTime.toString()
        
        val args = listOf(
            "ffmpeg", "-hide_banner", "-loglevel", "error",
            "-rtsp_transport", "tcp",
            "-i", rtspUrl,
            "-map", "0:v",
            "-c:v", "copy", "-an",
            "-f", "hls", 
            "-hls_time", "5", 
            "-hls_list_size", "5", 
            "-hls_flags", "delete_segments", 
            "$liveDir/index.m3u8",
            "-map", "0:v",
            "-c:v", "copy", "-an",
            "-f", "segment", 
            "-segment_time", segmentTime, 
            "-segment_atclocktime", "1", 
            "-strftime", "1", 
            "$backupDir/${stream.directory}_%Y-%m-%d_%H-%M-%S.mp4"
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

    fun stop() {
        scope.cancel()
    }

    fun isRecording(): Boolean {
        return recorders.values.any { it.isActive }
    }
}
