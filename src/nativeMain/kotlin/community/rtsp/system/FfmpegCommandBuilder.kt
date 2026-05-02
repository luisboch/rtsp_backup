package community.rtsp.system

class FfmpegCommandBuilder {
    private val args = mutableListOf<String>()

    fun hideBanner() = apply { args.add("-hide_banner") }
    
    fun logLevel(level: String) = apply {
        args.add("-loglevel")
        args.add(level)
    }

    fun rtspTransport(transport: String) = apply {
        args.add("-rtsp_transport")
        args.add(transport)
    }

    fun input(url: String) = apply {
        args.add("-i")
        args.add(url)
    }

    fun map(map: String) = apply {
        args.add("-map")
        args.add(map)
    }

    fun videoCodec(codec: String) = apply {
        args.add("-c:v")
        args.add(codec)
    }
    
    // Alias for -vcodec used in some places
    fun vcodec(codec: String) = apply {
        args.add("-vcodec")
        args.add(codec)
    }

    fun noAudio() = apply { args.add("-an") }

    fun format(format: String) = apply {
        args.add("-f")
        args.add(format)
    }

    fun hlsTime(seconds: Int) = apply {
        args.add("-hls_time")
        args.add(seconds.toString())
    }

    fun hlsListSize(size: Int) = apply {
        args.add("-hls_list_size")
        args.add(size.toString())
    }

    fun hlsFlags(flags: String) = apply {
        args.add("-hls_flags")
        args.add(flags)
    }

    fun segmentTime(seconds: String) = apply {
        args.add("-segment_time")
        args.add(seconds)
    }

    fun segmentAtClockTime(enabled: Boolean) = apply {
        args.add("-segment_atclocktime")
        args.add(if (enabled) "1" else "0")
    }

    fun strftime(enabled: Boolean) = apply {
        args.add("-strftime")
        args.add(if (enabled) "1" else "0")
    }
    
    fun pixFmt(fmt: String) = apply {
        args.add("-pix_fmt")
        args.add(fmt)
    }
    
    fun quality(q: Int) = apply {
        args.add("-q")
        args.add(q.toString())
    }
    
    fun frameRate(r: Int) = apply {
        args.add("-r")
        args.add(r.toString())
    }

    fun output(path: String) = apply {
        args.add(path)
    }

    fun build(): List<String> {
        return listOf("ffmpeg") + args
    }

    fun buildRaw(): List<String> {
        return args
    }

    fun addArg(arg: String) = apply {
        args.add(arg)
    }

    fun addArgs(vararg multipleArgs: String) = apply {
        args.addAll(multipleArgs)
    }
    
    fun buildCommandString(): String {
        return build().joinToString(" ") { arg ->
            val cleanArg = if (arg.startsWith("rtsp://")) arg.trim().removeSuffix("?") else arg
            "'" + cleanArg.replace("'", "'\\''") + "'"
        }
    }
}
