package community.rtsp.routes.util

import io.ktor.utils.io.ByteWriteChannel
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fread


@OptIn(ExperimentalForeignApi::class)
suspend fun ByteWriteChannel.streamFileToChannel(fd: CPointer<FILE>) {
    try {
        val buffer = ByteArray(65536)
        bufferedRead(buffer, fd)
    } finally {
        fclose(fd)
    }
}

@OptIn(ExperimentalForeignApi::class)
suspend fun ByteWriteChannel.bufferedRead(
    buffer: ByteArray,
    fd: CPointer<FILE>
) {
    while (true) {
        val read = memScoped {
            val nativeBuffer = allocArray<ByteVar>(buffer.size)
            val r = fread(nativeBuffer, 1u, buffer.size.toULong(), fd).toInt()
            if (r > 0) {
                for (i in 0 until r) buffer[i] = nativeBuffer[i]
            }
            r
        }
        if (read <= 0) break
        writeFully(buffer, 0, read)
        flush()
    }
}