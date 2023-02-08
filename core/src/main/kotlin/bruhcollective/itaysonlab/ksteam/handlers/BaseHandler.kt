package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket

/**
 * Describes a handler.
 *
 * A handler is a class which provides access to a specific API set and handles incoming packets.
 */
interface BaseHandler {
    /**
     * Handle default incoming [SteamPacket]s.
     */
    suspend fun onEvent(packet: SteamPacket) = Unit

    /**
     * Handle RPC [SteamPacket]s.
     *
     * These are packets which describe a RPC notification (defined in *.proto files).
     */
    suspend fun onRpcEvent(packet: SteamPacket) = Unit
}