package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket

/**
 * Describes a handler.
 *
 * A handler is a class which provides access to a specific API set and handles incoming packets.
 */
interface BaseHandler {
    /**
     * Handles default incoming [SteamPacket]s.
     */
    suspend fun onEvent(packet: SteamPacket) = Unit

    /**
     * Handles RPC [SteamPacket]s.
     *
     * These are packets which describe a RPC notification (defined in *.proto files).
     */
    suspend fun onRpcEvent(rpcMethod: String, packet: SteamPacket) = Unit

    /**
     * Handles SteamClient's stop function. Attached client instance is stopped forever with no ability to restart except for another instance creation.
     *
     * A handler should cancel any foreground work, if any, when [onClose] is called.
     */
    fun onClose() = Unit
}