package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import steam.enums.EMsg

/**
 * Manages Steam Guard, sessions and caches per-account
 */
class Storage(
    private val steamClient: SteamClient
): BaseHandler {
    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientLogOnResponse -> {
                // SteamID changed, load the correct storage
            }

            else -> {}
        }
    }
}