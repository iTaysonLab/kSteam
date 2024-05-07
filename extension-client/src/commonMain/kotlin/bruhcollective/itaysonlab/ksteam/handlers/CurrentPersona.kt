package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg

/**
 * Provides information about current signed in user, such as:
 * - VAC bans
 * - Wallet
 */
class CurrentPersona (
    steamClient: ExtendedSteamClient
) {
    init {
        steamClient.on(EMsg.k_EMsgClientWalletInfoUpdate) { packet ->
            // packet.getProtoPayload(CMsgClientWalletInfoUpdate.ADAPTER)
        }

        steamClient.on(EMsg.k_EMsgClientVacStatusResponse) { packet ->
            // [Ban Count] - [Banned AppID 1] - [Banned AppID 2] - ....
        }
    }
}