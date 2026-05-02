package community.rtsp.stream

import community.rtsp.auth.AuthRepository
import community.rtsp.config.AppConfig
import community.rtsp.db.Stream
import community.rtsp.system.FfmpegCommandBuilder
import kotlinx.coroutines.*
import platform.posix.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class StreamBackupService(
    private val config: AppConfig,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val recorders = mutableMapOf<String, Job>()

    fun start() {
        authRepository.getAllStreams().forEach { stream ->
            startStreamRecording(stream)
        }
    }

    fun startStreamRecording(stream: Stream) {
        if (recorders.containsKey(stream.alias)) return
        recorders[stream.alias] = scope.launch {
            runRecorder(stream)
        }
    }

    fun stopStreamRecording(alias: String) {
        recorders[alias]?.cancel()
        recorders.remove(alias)
    }

    private suspend fun CoroutineScope.runRecorder(stream: Stream) {
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

    private fun startFfmpeg(stream: Stream): Int {
        val cameraDir = "${config.dataDir}/${stream.owner_id}/${stream.directory}"
        val liveDir = "$cameraDir/live"
        val backupDir = "$cameraDir/backup"
        
        system("mkdir -p $liveDir")
        system("mkdir -p $backupDir")
        
        val rtspUrl = stream.rtsp_url.trim().removeSuffix("?")
        val segmentTime = config.properties.segmentTime.toString()
        
        val args = FfmpegCommandBuilder()
            .hideBanner()
            .logLevel("error")
            .rtspTransport("tcp")
            .input(rtspUrl)
            .map("0:v")
            .videoCodec("copy")
            .noAudio()
            .format("hls")
            .hlsTime(5)
            .hlsListSize(5)
            .hlsFlags("delete_segments")
            .output("$liveDir/index.m3u8")
            .map("0:v")
            .videoCodec("copy")
            .noAudio()
            .format("segment")
            .segmentTime(segmentTime)
            .segmentAtClockTime(true)
            .strftime(true)
            .output("$backupDir/${stream.directory}_%Y-%m-%d_%H-%M-%S.mp4")
            .build()

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
