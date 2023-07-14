package bruhcollective.itaysonlab.ksteam.messages

import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import okio.BufferedSink
import okio.BufferedSource
import steam.webui.common.CMsgProtoBufHeader

sealed interface SteamPacketHeader {
    fun read(buffer: BufferedSource)
    fun write(buffer: BufferedSink)

    var targetJobId: Long
    var sourceJobId: Long
    var steamId: ULong
    var sessionId: Int

    class Binary : SteamPacketHeader {
        private var headerSize: Byte = 36
        private var headerVersion: Int = 2

        override var targetJobId: Long = Long.MAX_VALUE
        override var sourceJobId: Long = Long.MAX_VALUE

        private var headerCanary: UByte = 239u

        override var steamId: ULong = 0u
        override var sessionId: Int = 0

        override fun read(buffer: BufferedSource) {
            headerSize = buffer.readByte()
            headerVersion = buffer.readIntLe()
            targetJobId = buffer.readLongLe()
            sourceJobId = buffer.readLongLe()
            headerCanary = buffer.readByte().toUByte()
            steamId = buffer.readLongLe().toULong()
            sessionId = buffer.readIntLe()
        }

        override fun write(buffer: BufferedSink) {
            buffer.writeByte(headerSize.toInt())
            buffer.writeIntLe(headerVersion)
            buffer.writeLongLe(targetJobId)
            buffer.writeLongLe(sourceJobId)
            buffer.writeByte(headerCanary.toInt())
            buffer.writeLongLe(steamId.toLong())
            buffer.writeIntLe(sessionId)
        }

        override fun toString(): String {
            return "Binary(headerSize=$headerSize, headerVersion=$headerVersion, targetJobId=$targetJobId, sourceJobId=$sourceJobId, headerCanary=$headerCanary, steamId=$steamId, sessionId=$sessionId)"
        }
    }

    class Protobuf : SteamPacketHeader {
        private var protoHeader = CMsgProtoBufHeader()

        override var targetJobId: Long
            get() = protoHeader.jobid_target ?: 0L
            set(value) {
                protoHeader = protoHeader.copy(jobid_target = value)
            }

        override var sourceJobId: Long
            get() = protoHeader.jobid_source ?: 0L
            set(value) {
                protoHeader = protoHeader.copy(jobid_source = value)
            }

        override var steamId: ULong
            get() = protoHeader.steamid?.toULong() ?: 0u
            set(value) {
                protoHeader = protoHeader.copy(steamid = value.toLong())
            }

        override var sessionId: Int
            get() = protoHeader.client_sessionid ?: 0
            set(value) {
                protoHeader = protoHeader.copy(client_sessionid = value)
            }

        var targetJobName: String?
            get() = protoHeader.target_job_name
            set(value) {
                protoHeader = protoHeader.copy(target_job_name = value)
            }

        val result: EResult get() = EResult.byEncoded(protoHeader.eresult ?: EResult.Fail.encoded)

        override fun read(buffer: BufferedSource) {
            val headerLength = buffer.readIntLe().toLong()
            val headerByteArray = buffer.readByteArray(headerLength)
            protoHeader = CMsgProtoBufHeader.ADAPTER.decode(headerByteArray)
        }

        override fun write(buffer: BufferedSink) {
            CMsgProtoBufHeader.ADAPTER.encode(protoHeader).let { protoData ->
                buffer.writeIntLe(protoData.size)
                buffer.write(protoData)
            }
        }

        override fun toString(): String {
            return protoHeader.toString()
        }
    }
}