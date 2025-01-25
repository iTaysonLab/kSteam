package bruhcollective.itaysonlab.ksteam.util

import okio.*

/**
 * A small utility class that compressed [ByteArray] and is shared in context
 */
internal class SharedCompressor: Closeable {
    private val buffer = Buffer()

    fun compress(data: ByteArray): ByteArray {
        buffer.clear()
        (buffer as BufferedSink).gzip().buffer().use { sink -> sink.write(data) }
        return (buffer as BufferedSource).readByteArray()
    }

    override fun close() {
        buffer.clear()
        buffer.close()
    }
}