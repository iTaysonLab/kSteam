package bruhcollective.itaysonlab.ksteam.messages

import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.models.Result
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import com.squareup.wire.ProtoAdapter
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink

/**
 * Definition of a packet going through Steam Network.
 *
 * **Packet structure:**
 * < Message ID > - < Header > - < Payload >
 *
 * There are two types of packets:
 * - **protobuf**, using a protobuf header and glued protobuf content
 * - **binary**, using a binary header and content
 *
 * Type depends on chosen EMsg (protobuf ones uses a special mask on their ID)
 */
class SteamPacket private constructor(
    val messageId: EMsg,
    val header: SteamPacketHeader,
    var payload: ByteArray
) {
    companion object {
        // Whitelist of EMsg ID's that can be executed without auth
        private val anonymousIds =
            arrayOf(
                EMsg.k_EMsgClientHello,
                EMsg.k_EMsgServiceMethodCallFromClientNonAuthed,
                EMsg.k_EMsgClientLogon
            )

        // Usage: EMsg and ProtobufMask
        private const val ProtobufMask = 0x80000000.toInt()

        // Usage: Int and ProtobufClearMask
        private const val ProtobufClearMask = ProtobufMask.inv()

        internal fun canBeExecutedWithoutAuth(packet: SteamPacket): Boolean {
            return packet.messageId in anonymousIds
        }

        /**
         * Parses a [ByteArray] into a [SteamPacket].
         *
         * The byte array should be a direct copy of traffic, with the message ID and header glued.
         *
         * @param rawPacket raw packet bytes
         */
        fun ofNetworkPacket(rawPacket: ByteArray): SteamPacket {
            require(rawPacket.size >= 4) { "Packet is not valid (too small)" }

            val packetBuffer = rawPacket.buffer()
            val messageIdRaw = packetBuffer.readIntLe()
            val messageId = EMsg.byEncoded(messageIdRaw and ProtobufClearMask)

            KSteamLogging.logVerbose("SteamPacket:ParseNet") {
                "Received message: $messageId (protobuf: ${(messageIdRaw and ProtobufMask) != 0})"
            }

            val header: SteamPacketHeader = if ((messageIdRaw and ProtobufMask) != 0) {
                SteamPacketHeader.Protobuf()
            } else {
                SteamPacketHeader.Binary()
            }.apply { read(packetBuffer) }

            KSteamLogging.logVerbose("SteamPacket:ParseNet") { "> [header] $header" }

            val payload = packetBuffer.readByteArray()

            return SteamPacket(
                messageId = messageId,
                header = header,
                payload = payload
            )
        }

        /**
         * Creates a new [SteamPacket] with a protobuf content
         *
         * @param messageId message ID of the packet
         * @param adapter wire protobuf adapter for the payload
         * @param payload a payload - object which will be encoded in the packet
         */
        fun <T> newProto(messageId: EMsg, adapter: ProtoAdapter<T>, payload: T): SteamPacket {
            // require((messageId.encoded and ProtobufMask) != 0) { "Provided messageId is not applicable to protobuf packets: $messageId" }
            return SteamPacket(
                messageId = messageId,
                header = SteamPacketHeader.Protobuf(),
                payload = adapter.encode(payload)
            )
        }
    }

    /**
     * Changes [SteamPacketHeader] of a specific [SteamPacket].
     *
     * For example, you can explicitly set the [bruhcollective.itaysonlab.ksteam.models.SteamId] of a specific packet.
     */
    fun withHeader(func: SteamPacketHeader.() -> Unit): SteamPacket {
        header.apply(func)
        return this
    }

    /**
     * Encodes the content of this packet into a [ByteArray].
     *
     * @return [ByteArray] with the content
     */
    fun encode(): ByteArray = Buffer().apply {
        writeTo(this)
    }.readByteArray()

    /**
     * Writes the content of this packet into a [Sink]
     *
     * @param sink okio sink to write into
     */
    fun writeTo(sink: BufferedSink) = sink.apply {
        writeIntLe(messageId.encoded.let {
            if (header is SteamPacketHeader.Protobuf) {
                it or ProtobufMask
            } else {
                it
            }
        })

        header.write(this)
        write(payload)
    }

    /**
     * Decodes the protobuf packet content into a object.
     *
     * @param adapter wire protobuf adapter for the content
     */
    fun <T> getProtoPayload(adapter: ProtoAdapter<T>): Result<T> {
        require(header is SteamPacketHeader.Protobuf) { "Message is not protobuf, but proto decoding requested" }

        return Result(
            try {
                adapter.decode(payload)
            } catch (e: Exception) {
                null
            } to header.result
        )
    }

    fun getBinaryPayload(): BufferedSource {
        require(header is SteamPacketHeader.Binary) { "Message is not binary, but binary decoding requested" }
        return payload.buffer()
    }

    fun isProtobuf() = header is SteamPacketHeader.Protobuf
    fun isBinary() = header is SteamPacketHeader.Binary
}

private fun ByteArray.buffer() = Buffer().write(this)