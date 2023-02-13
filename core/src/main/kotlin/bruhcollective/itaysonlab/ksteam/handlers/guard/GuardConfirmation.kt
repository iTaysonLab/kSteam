package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.guard.GuardInstance
import bruhcollective.itaysonlab.ksteam.guard.models.ConfirmationListState
import bruhcollective.itaysonlab.ksteam.guard.models.MobileConfResult
import bruhcollective.itaysonlab.ksteam.guard.models.MobileConfirmationItem
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
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
                steamClient.externalWebApi.ajaxGet(
                    path = listOf("mobileconf", "getlist"),
                    parameters = mapOf(
                        "p" to steamClient.config.deviceInfo.uuid,
                        "a" to instance.steamId.longId.toString(),
                        "t" to sigStamp.generationTime.toString(),
                        "k" to sigStamp.b64EncodedSignature,
                        "m" to "react",
                        "tag" to "list"
                    )
                ).bodyAsText().let {
                    ConfirmationListState.Decoder.decodeFromString(it)
                }
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
            steamClient.externalWebApi.ajaxGetTyped<MobileConfResult>(
                path = listOf("mobileconf", "ajaxop"),
                parameters = mapOf(
                    "p" to steamClient.config.deviceInfo.uuid,
                    "a" to instance.steamId.longId.toString(),
                    "t" to sigStamp.generationTime.toString(),
                    "k" to sigStamp.b64EncodedSignature,
                    "m" to "react",
                    "tag" to tag,
                    "op" to operation,
                    "cid" to item.id,
                    "ck" to item.nonce
                )
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