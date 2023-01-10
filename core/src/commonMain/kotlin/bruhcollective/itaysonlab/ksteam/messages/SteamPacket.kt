package bruhcollective.itaysonlab.ksteam.messages

import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.models.Result
import bruhcollective.itaysonlab.ksteam.util.buffer
import com.squareup.wire.ProtoAdapter
import okio.Buffer
import okio.ByteString.Companion.toByteString
import steam.enums.EMsg
import steam.extra.enums.EResult

/**
 * Structure:
 * [kMsg*][  header ][ message ]
 * - int --  bytes  --  bytes  -
 *
 * packet can be both proto/structure-based
 * proto has a protobuf header + message glued
 */
class SteamPacket private constructor(val messageId: EMsg, val header: SteamPacketHeader, private var payload: ByteArray) {
    companion object {
        // Usage: EMsg and ProtobufMask
        private const val ProtobufMask = 0x80000000.toInt()

        // Usage: Int and ProtobufClearMask
        private const val ProtobufClearMask = ProtobufMask.inv()

        fun ofNetworkPacket(rawPacket: ByteArray): SteamPacket {
            require(rawPacket.size >= 4) { "Packet is not valid (too small)" }

            val packetBuffer = rawPacket.buffer()
            val messageIdRaw = packetBuffer.readIntLe()
            val messageId = EMsg.fromValue(messageIdRaw and ProtobufClearMask)

            logDebug("SteamPacket:ParseNet", "Received packet of ID $messageId (proto: ${(messageIdRaw and ProtobufMask) != 0})")

            val header: SteamPacketHeader = if ((messageIdRaw and ProtobufMask) != 0) {
                SteamPacketHeader.Protobuf()
            } else {
                SteamPacketHeader.Binary()
            }.apply { read(packetBuffer) }

            logDebug("SteamPacket:ParseNet", "[header] > $header")

            val payload = packetBuffer.readByteArray()

            logDebug("SteamPacket:ParseNet", "[payload] > ${payload.toByteString().hex()}")

            return SteamPacket(
                messageId = messageId ?: EMsg.k_EMsgInvalid,
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

    fun encode(): ByteArray = Buffer().apply {
        writeIntLe(messageId.value.let {
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

        return if (header.result == EResult.OK) {
            Result(adapter.decode(payload) to EResult.OK)
        } else {
            Result(null to header.result)
        }
    }

    fun <T> getBinaryPayload(adapter: SteamBinaryPayloadAdapter<T>): T {
        require(header is SteamPacketHeader.Binary) { "Message is not binary, but binary decoding requested" }
        return adapter.decode(payload.buffer())
    }
}