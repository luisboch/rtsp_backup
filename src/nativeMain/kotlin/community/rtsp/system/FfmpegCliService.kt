package community.rtsp.system

import platform.posix.system

class FfmpegCliService {
    fun isAvailable(): Boolean {
        return system("ffmpeg -version >/dev/null 2>&1") == 0
    }
}
