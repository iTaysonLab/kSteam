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
    val information: CMJobInformation

    val deferred: CompletableDeferred<T>

    suspend fun accept(packet: SteamPacket): Boolean

    fun failRemote(result: EResult) {
        deferred.completeExceptionally(CMJobRemoteException(information, result))
    }

    fun failTimeout() {
        deferred.completeExceptionally(CMJobTimeoutException(information))
    }

    fun failDropped(reason: CMJobDroppedException.Reason) {
        deferred.completeExceptionally(CMJobDroppedException(information, reason))
    }

    /**
     * A single job instance that consumes a [ByteArray].
     *
     * @param information common job information
     */
    data class Single (
        override val information: CMJobInformation
    ): CMJob<SteamPacket> {
        override val deferred = CompletableDeferred<SteamPacket>()

        override suspend fun accept(packet: SteamPacket): Boolean {
            deferred.complete(packet)
            return true
        }
    }

    /**
     * A single job instance that consumes a protobuf message.
     *
     * @param information common job information
     * @param adapter [ProtoAdapter] to auto-parse the payload
     */
    data class SingleProtobuf <T> (
        override val information: CMJobInformation,
        val adapter: ProtoAdapter<T>
    ): CMJob<T> {
        override val deferred: CompletableDeferred<T> = CompletableDeferred()

        override suspend fun accept(packet: SteamPacket): Boolean {
            deferred.complete(adapter.decode(packet.payload))
            return true
        }
    }

    /**
     * A multiple job instance that consumes protobuf messages.
     *
     * @param information common job information
     * @param adapter [ProtoAdapter] to auto-parse the payload
     * @param stopIf returning true will complete this job
     */
    data class MultipleProtobuf <T> (
        override val information: CMJobInformation,
        val adapter: ProtoAdapter<T>,
        val stopIf: (T) -> Boolean
    ): CMJob<List<T>> {
        private val data = mutableListOf<T>()

        override val deferred: CompletableDeferred<List<T>> = CompletableDeferred()

        override suspend fun accept(packet: SteamPacket): Boolean {
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

    /**
     * A multiple job instance that consumes protobuf messages with streaming capabilities.
     *
     * This means that instead of collecting all messages into a list, every collected message will be processed right on receive.
     *
     * **Note:** This is an experimental CMJob.
     * You probably don't want to use this, unless you are working with REALLY large responses.
     * For example, a PICS query might benefit from this.
     *
     * @param information common job information
     * @param adapter [ProtoAdapter] to auto-parse the payload
     * @param process a processing lambda, returning true will complete the job. NOTE: new packets won't be processed until this lambda will finish running.
     */
    data class MultipleStreamingProtobuf <T> (
        override val information: CMJobInformation,
        val adapter: ProtoAdapter<T>,
        val process: suspend (T) -> Boolean
    ): CMJob<Unit> {
        override val deferred: CompletableDeferred<Unit> = CompletableDeferred()

        override suspend fun accept(packet: SteamPacket): Boolean {
            if (process(adapter.decode(packet.payload))) {
                deferred.complete(Unit)
                return true
            } else {
                return false
            }
        }
    }
}