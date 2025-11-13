package bruhcollective.itaysonlab.ksteam.network.event

import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

/**
 * An outgoing event manager for CM clients.
 */
internal class OutgoingPacketManager (
    val logger: Logger
) {
    private companion object {
        const val TAG = "OutgoingPacketManager"
    }

    // Currently available queue
    private val queue: Channel<SteamPacket> = Channel(capacity = Channel.BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)

    /**
     * Launches a new outgoing packet collection job.
     */
    suspend fun listenOutgoingPackets(onPacket: suspend (SteamPacket) -> Unit) {
        logger.logDebug(TAG) { "[listenOutgoingPackets]" }

        queue.consumeEach { packet ->
            onPacket(packet)
        }
    }

    /**
     * Explicitly clears the outgoing queue, dropping ALL packets.
     */
    suspend fun clean() {
        logger.logDebug(TAG) { "[clean]" }

        // Clear
        var stillHasElements = true
        while (stillHasElements) { stillHasElements = queue.tryReceive().isSuccess }
    }

    /**
     * Adds a new packet to the outgoing queue.
     */
    suspend fun enqueuePacket(packet: SteamPacket) {
        logger.logDebug(TAG) { "[enqueuePacket] $packet" }
        queue.send(packet)
    }
}