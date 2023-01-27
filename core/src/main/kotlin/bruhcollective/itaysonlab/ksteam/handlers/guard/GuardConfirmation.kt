package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.guard.GuardInstance
import bruhcollective.itaysonlab.ksteam.guard.models.ConfirmationListState
import bruhcollective.itaysonlab.ksteam.guard.models.MobileConfirmationItem
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * Mobile confirmations using Steam Guard instances. (trade/market)
 */
class GuardConfirmation(
    private val steamClient: SteamClient
) : BaseHandler {
    /**
     * Get a list of confirmations waiting for the response.
     */
    suspend fun getConfirmations(instance: GuardInstance): ConfirmationListState {
        return try {
            instance.confirmationTicket("list").let { sigStamp ->
                steamClient.externalWebApi.getConfirmations(
                    steamId = instance.steamId.longId,
                    timestamp = sigStamp.generationTime,
                    signature = sigStamp.b64EncodedSignature,
                    platform = steamClient.config.deviceInfo.uuid
                )
            }
        } catch (e: Exception) {
            ConfirmationListState.NetworkError(e)
        }
    }

    /**
     * Approves or rejects a confirmation.
     *
     * @return if the operation is success
     */
    suspend fun setConfirmationStatus(instance: GuardInstance, item: MobileConfirmationItem, allow: Boolean): Boolean {
        val tag = if (allow) {
            "accept"
        } else {
            "reject"
        }

        val operation = if (allow) {
            "allow"
        } else {
            "cancel"
        }

        return instance.confirmationTicket(tag).let { sigStamp ->
            steamClient.externalWebApi.runConfOperation(
                steamId = instance.steamId.longId,
                timestamp = sigStamp.generationTime,
                signature = sigStamp.b64EncodedSignature,
                platform = steamClient.config.deviceInfo.uuid,
                cid = item.id,
                ck = item.nonce,
                tag = tag,
                operation = operation
            ).success
        }
    }

    /**
     * Generates a page URL to display in clients.
     */
    suspend fun generateDetailPageUrl(
        instance: GuardInstance,
        item: MobileConfirmationItem
    ): String {
        val sigStamp = instance.confirmationTicket("detail")
        val b64 = withContext(Dispatchers.IO) { URLEncoder.encode(sigStamp.b64EncodedSignature, "UTF-8") }
        return "https://steamcommunity.com/mobileconf/detailspage/${item.id}?p=${steamClient.config.deviceInfo.uuid}&a=${instance.steamId.longId}&k=$b64&t=${sigStamp.generationTime}&m=react&tag=detail"
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit
}