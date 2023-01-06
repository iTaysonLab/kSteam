package bruhcollective.itaysonlab.ksteam.messages

import bruhcollective.itaysonlab.ksteam.models.JobId
import steam.enums.EMsg

abstract class BasePacketMessage <T> (
    internal val messageId: EMsg
) {
    internal var sourceJobId: JobId? = null
    internal var targetJobId: JobId? = null

    var payload: T? = null

    fun fromSteamPacket(bytes: ByteArray) {
        // data =
    }

    fun toSteamPacket(): ByteArray {
        return byteArrayOf()
    }

    fun withPayload(payload: T) = apply {
        this.payload = payload
    }
}