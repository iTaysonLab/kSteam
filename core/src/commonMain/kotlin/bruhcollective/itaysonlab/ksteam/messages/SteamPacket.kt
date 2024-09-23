package bruhcollective.itaysonlab.ksteam.messages

import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.util.EnumCache
import com.squareup.wire.Message
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
class SteamPacket (
    val messageId: EMsg,
    val header: SteamPacketHeader,
    val payload: ByteArray
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
            val messageId = EnumCache.eMsg(messageIdRaw and ProtobufClearMask)

            val header: SteamPacketHeader = if ((messageIdRaw and ProtobufMask) != 0) {
                SteamPacketHeader.Protobuf()
            } else {
                SteamPacketHeader.Binary()
            }.apply { read(packetBuffer) }

            return SteamPacket(
                messageId = messageId,
                header = header,
                payload = packetBuffer.readByteArray() // TODO: figure out how can we avoid double-buffering
            )
        }

        /**
         * Creates a new [SteamPacket] with a protobuf content
         *
         * @param messageId message ID of the packet
         * @param adapter wire protobuf adapter for the payload
         * @param payload a payload - object which will be encoded in the packet
         */
        @Deprecated(
            message = "Wire now provides adapters inside message classes, specifying ProtoAdapter separately is no longer required.",
            replaceWith = ReplaceWith("newProto(messageId, payload)")
        )
        fun <T> newProto(messageId: EMsg, adapter: ProtoAdapter<T>, payload: T): SteamPacket {
            return SteamPacket(
                messageId = messageId,
                header = SteamPacketHeader.Protobuf(),
                payload = adapter.encode(payload)
            )
        }

        /**
         * Creates a new [SteamPacket] with a protobuf content
         *
         * @param messageId message ID of the packet
         * @param adapter wire protobuf adapter for the payload
         * @param payload a payload - object which will be encoded in the packet
         */
        fun <T: Message<T, *>> newProto(messageId: EMsg, payload: T): SteamPacket {
            return SteamPacket(
                messageId = messageId,
                header = SteamPacketHeader.Protobuf(),
                payload = payload.adapter.encode(payload)
            )
        }
    }

    /**
     * Changes [SteamPacketHeader] of a specific [SteamPacket].
     *
     * For example, you can explicitly set the [bruhcollective.itaysonlab.ksteam.models.SteamId] of a specific packet.
     */
    fun withHeader(func: SteamPacketHeader.() -> Unit): SteamPacket = apply {
        header.apply(func)
    }

    /**
     * Encodes the content of this packet into a [ByteArray].
     *
     * @return [ByteArray] with the content
     */
    fun encode(): ByteArray = Buffer().apply(::writeTo).readByteArray()

    /**
     * Writes the content of this packet into a [Sink].
     *
     * @param sink Okio sink to write into
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
     * Returns the buffer of a payload for easier byte-by-byte reading.
     */
    fun buffer(): BufferedSource = payload.buffer()

    /**
     * Returns if this message is a protobuf-encoded one.
     */
    fun isProtobuf() = header is SteamPacketHeader.Protobuf

    /**
     * Returns if this message is a binary-encoded one.
     */
    fun isBinary() = header is SteamPacketHeader.Binary

    /**
     * [EResult] of a request. Only applicable to protobuf messages, otherwise returns [EResult.Fail].
     *
     * Possible returns:
     * - if the packet was not related to any outbound request (job ID is 0), [EResult.OK] will be returned because incoming packets can't be failed
     * - if the packet has a protobuf header, it returns corresponding [EResult]
     * - otherwise, [EResult.Fail] will be returned
     */
    val result: EResult get() {
        return if (header.targetJobId != 0L) {
            (header as? SteamPacketHeader.Protobuf)?.result ?: EResult.Fail
        } else {
            EResult.OK
        }
    }

    /**
     * Returns if this packet resulted in a success. Only applicable to protobuf messages.
     */
    val success: Boolean get() = result == EResult.OK

    /**
     * Returns if this packet resulted in an error. Only applicable to protobuf messages.
     */
    val error: Boolean get() = result != EResult.OK
}

private fun ByteArray.buffer() = Buffer().write(this)