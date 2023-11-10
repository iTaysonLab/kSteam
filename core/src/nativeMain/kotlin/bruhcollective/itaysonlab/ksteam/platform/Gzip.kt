package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.cinterop.*
import libdeflate.libdeflate_alloc_decompressor
import libdeflate.libdeflate_deflate_decompress
import libdeflate.libdeflate_free_decompressor
import okio.Buffer
import okio.Source
import okio.buffer
import okio.use

@OptIn(ExperimentalForeignApi::class)
actual fun Source.readGzippedContentAsBuffer(knownUnzippedSize: Int?): Source = use {
    val buffer = Buffer()

    val srcBytes = buffer().readByteArray()
    val srcBytesFallbackSize = (knownUnzippedSize ?: (srcBytes.size * 4)).toULong()

    val outBytes = ByteArray(srcBytesFallbackSize.toInt())

    memScoped {
        val decompressor = libdeflate_alloc_decompressor() ?: error("libdeflate decompressor is not init'ed properly")
        val actualBytes = alloc<ULongVar>()

        srcBytes.usePinned { _src ->
            outBytes.usePinned { _out ->
                val srcAddress = _src.addressOf(0)
                val outAddress = _out.addressOf(0)

                libdeflate_deflate_decompress(
                    decompressor = decompressor,
                    `in` = srcAddress,
                    in_nbytes = srcBytes.size.toULong(),
                    out = outAddress,
                    out_nbytes_avail = srcBytesFallbackSize,
                    actual_out_nbytes_ret = if (knownUnzippedSize != null) {
                        null
                    } else {
                        actualBytes.ptr
                    }
                ).let {
                    require(it == 0u) {
                        "libdeflate_deflate_decompress returned non-LIBDEFLATE_SUCCESS result: $it"
                    }
                }

                if (knownUnzippedSize != null) {
                    buffer.write(outBytes)
                } else {
                    buffer.write(outBytes, 0, actualBytes.value.toInt())
                }
            }
        }

        libdeflate_free_decompressor(decompressor)
    }

    buffer
}