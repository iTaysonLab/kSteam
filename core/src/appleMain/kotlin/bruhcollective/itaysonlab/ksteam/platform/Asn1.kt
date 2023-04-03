package bruhcollective.itaysonlab.ksteam.platform

import bruhcollective.itaysonlab.ksteam.util.HexNumbers
import okio.Buffer
import okio.BufferedSink

// Very simple ASN.1 writer
value class Asn1Writer (private val sink: BufferedSink) {
    fun sequence(scope: Asn1Writer.() -> Unit) {
        createAsn1(scope).apply {
            sink.writeByte(0x30) // SEQUENCE type
            sink.writeInt(size)
            sink.write(this)
        }
    }

    fun hexInteger(value: String) {
        sink.writeByte(0x02) // INTEGER type
        sink.writeByte(0x01) // byte size
        sink.write(HexNumbers.toByteArray(value))
    }
}

fun createAsn1(block: Asn1Writer.() -> Unit): ByteArray {
    return Buffer().apply {
        Asn1Writer(this).let(block)
    }.readByteArray()
}