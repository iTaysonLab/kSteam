package bruhcollective.itaysonlab.ksteam.messages

import okio.Buffer
import steam.extra.enums.EResult
import steam.messages.base.CMsgProtoBufHeader

sealed class SteamPacketHeader private constructor() {
    abstract fun read(buffer: Buffer)
    abstract fun write(buffer: Buffer)

    abstract var targetJobId: Long
    abstract var sourceJobId: Long
    abstract var steamId: ULong
    abstract var sessionId: Int

    class Binary: SteamPacketHeader() {
        private var headerSize: Byte = 36
        private var headerVersion: Int = 2

        override var targetJobId: Long = Long.MAX_VALUE
        override var sourceJobId: Long = Long.MAX_VALUE

        private var headerCanary: UByte = 239u

        override var steamId: ULong = 0u
        override var sessionId: Int = 0

        override fun read(buffer: Buffer) {
            headerSize = buffer.readByte()
            headerVersion = buffer.readIntLe()
            targetJobId = buffer.readLongLe()
            sourceJobId = buffer.readLongLe()
            headerCanary = buffer.readByte().toUByte()
            steamId = buffer.readLongLe().toULong()
            sessionId = buffer.readIntLe()
        }

        override fun write(buffer: Buffer) {
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

    class Protobuf: SteamPacketHeader() {
        private var protoHeader = CMsgProtoBufHeader(realm = 1)

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

        val result: EResult get() = protoHeader.eresult ?: EResult.Fail

        override fun read(buffer: Buffer) {
            val headerLength = buffer.readIntLe().toLong()
            val headerByteArray = buffer.readByteArray(headerLength)
            protoHeader = CMsgProtoBufHeader.ADAPTER.decode(headerByteArray)
        }

        override fun write(buffer: Buffer) {
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