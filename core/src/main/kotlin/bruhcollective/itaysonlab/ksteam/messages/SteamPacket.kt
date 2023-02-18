package bruhcollective.itaysonlab.ksteam.messages

import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.models.Result
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.util.buffer
import com.squareup.wire.ProtoAdapter
import okio.Buffer

/**
 * Structure:
 * [kMsg*][  header ][ message ]
 * - int --  bytes  --  bytes  -
 *
 * packet can be both proto/structure-based
 * proto has a protobuf header + message glued
 */
class SteamPacket private constructor(
    val messageId: EMsg,
    val header: SteamPacketHeader,
    var payload: ByteArray
) {
    companion object {
        // Whitelist of EMsg ID's that can be executed without auth
        private val anonymousIds =
            arrayOf(EMsg.k_EMsgClientHello, EMsg.k_EMsgServiceMethodCallFromClientNonAuthed, EMsg.k_EMsgClientLogon)

        // Usage: EMsg and ProtobufMask
        private const val ProtobufMask = 0x80000000.toInt()

        // Usage: Int and ProtobufClearMask
        private const val ProtobufClearMask = ProtobufMask.inv()

        fun canBeExecutedWithoutAuth(packet: SteamPacket): Boolean {
            return packet.messageId in anonymousIds
        }

        fun ofNetworkPacket(rawPacket: ByteArray): SteamPacket {
            require(rawPacket.size >= 4) { "Packet is not valid (too small)" }

            val packetBuffer = rawPacket.buffer()
            val messageIdRaw = packetBuffer.readIntLe()
            val messageId = EMsg.byEncoded(messageIdRaw and ProtobufClearMask)

            logVerbose(
                "SteamPacket:ParseNet",
                "Received message: $messageId (protobuf: ${(messageIdRaw and ProtobufMask) != 0})"
            )

            val header: SteamPacketHeader = if ((messageIdRaw and ProtobufMask) != 0) {
                SteamPacketHeader.Protobuf()
            } else {
                SteamPacketHeader.Binary()
            }.apply { read(packetBuffer) }

            logVerbose("SteamPacket:ParseNet", "> [header] $header")

            val payload = packetBuffer.readByteArray()

            return SteamPacket(
                messageId = messageId,
                header = header,
                payload = payload
            )
        }

        fun <T> newProto(messageId: EMsg, adapter: ProtoAdapter<T>, payload: T): SteamPacket {
            return SteamPacket(
                messageId = messageId,
                header = SteamPacketHeader.Protobuf(),
                payload = adapter.encode(payload)
            )
        }
    }

    fun withHeader(func: SteamPacketHeader.() -> Unit): SteamPacket {
        header.apply(func)
        return this
    }

    fun encode(): ByteArray = Buffer().apply {
        writeIntLe(messageId.encoded.let {
            if (header is SteamPacketHeader.Protobuf) {
                it or ProtobufMask
            } else {
                it
            }
        })

        header.write(this)
        write(payload)
    }.readByteArray()

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

    fun <T> getBinaryPayload(adapter: SteamBinaryPayloadAdapter<T>): T {
        require(header is SteamPacketHeader.Binary) { "Message is not binary, but binary decoding requested" }
        return adapter.decode(payload.buffer())
    }

    fun isProtobuf() = header is SteamPacketHeader.Protobuf
    fun isBinary() = header is SteamPacketHeader.Binary
}