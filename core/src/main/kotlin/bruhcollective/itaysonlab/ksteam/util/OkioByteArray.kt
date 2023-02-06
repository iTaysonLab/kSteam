package bruhcollective.itaysonlab.ksteam.util

import okio.*

internal fun ByteArray.buffer() = Buffer().write(this)

// why
internal fun ByteString.compress(): ByteArray {
    return Buffer().also { buffer ->
        (buffer as BufferedSink).gzip().buffer().write(this)
    }.readByteArray()
}

internal fun ByteArray.decompress(): ByteArray {
    return Buffer().write(this).let { buffer ->
        (buffer as BufferedSource).gzip().buffer().readByteArray()
    }
}