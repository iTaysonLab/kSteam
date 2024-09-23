package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobDroppedException
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobRemoteException
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobTimeoutException
import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.CompletableDeferred

/**
 * A "job" describes a Steam Network request.
 */
internal sealed interface CMJob <T> {
    val id: CMJobId
    val deferred: CompletableDeferred<T>

    fun accept(packet: SteamPacket): Boolean

    fun failRemote(result: EResult) {
        deferred.completeExceptionally(CMJobRemoteException(id, result))
    }

    fun failTimeout() {
        deferred.completeExceptionally(CMJobTimeoutException(id))
    }

    fun failDropped() {
        deferred.completeExceptionally(CMJobDroppedException(id))
    }

    /**
     * A single job instance that consumes a [ByteArray].
     */
    data class Single (
        override val id: CMJobId
    ): CMJob<SteamPacket> {
        override val deferred = CompletableDeferred<SteamPacket>()

        override fun accept(packet: SteamPacket): Boolean {
            deferred.complete(packet)
            return true
        }
    }

    /**
     * A single job instance that consumes a protobuf message.
     */
    data class SingleProtobuf <T> (
        override val id: CMJobId,
        val adapter: ProtoAdapter<T>
    ): CMJob<T> {
        override val deferred: CompletableDeferred<T> = CompletableDeferred()

        override fun accept(packet: SteamPacket): Boolean {
            deferred.complete(adapter.decode(packet.payload))
            return true
        }
    }

    /**
     * A multiple job instance that consumes a protobuf message.
     */
    data class MultipleProtobuf <T> (
        override val id: CMJobId,
        val adapter: ProtoAdapter<T>,
        val stopIf: (T) -> Boolean
    ): CMJob<List<T>> {
        private val data = mutableListOf<T>()

        override val deferred: CompletableDeferred<List<T>> = CompletableDeferred()

        override fun accept(packet: SteamPacket): Boolean {
            val parsedMessage = adapter.decode(packet.payload)
            data += parsedMessage

            if (stopIf(parsedMessage)) {
                deferred.complete(data)
                return true
            } else {
                return false
            }
        }
    }
}