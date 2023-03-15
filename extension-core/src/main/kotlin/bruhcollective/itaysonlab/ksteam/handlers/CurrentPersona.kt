package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import steam.webui.common.CMsgClientWalletInfoUpdate

/**
 * Provides information about current signed in user, such as:
 * - VAC bans
 * - Wallet
 */
class CurrentPersona: BaseHandler {
    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientWalletInfoUpdate -> {
                packet.getProtoPayload(CMsgClientWalletInfoUpdate.ADAPTER).dataNullable?.apply {
                    // println(this)
                }
            }

            else -> Unit
        }
    }
}