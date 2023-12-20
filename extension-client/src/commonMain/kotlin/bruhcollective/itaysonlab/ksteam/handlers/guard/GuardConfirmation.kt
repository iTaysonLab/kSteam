package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.guard.GuardInstance
import bruhcollective.itaysonlab.ksteam.guard.models.ConfirmationListState
import bruhcollective.itaysonlab.ksteam.guard.models.MobileConfResult
import bruhcollective.itaysonlab.ksteam.guard.models.MobileConfirmationItem
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.configuration
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import io.ktor.client.statement.*
import io.ktor.http.*

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
                steamClient.webApi.community.method("mobileconf/getlist") {
                    "p" with steamClient.configuration.getUuid()
                    "a" with instance.steamId.longId
                    "t" with sigStamp.generationTime
                    "k" with sigStamp.b64EncodedSignature
                    "m" with "react"
                    "tag" with "list"
                }.get().bodyAsText().let {
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
            steamClient.webApi.community.method("mobileconf/ajaxop") {
                "p" with steamClient.configuration.getUuid()
                "a" with instance.steamId.longId.toString()
                "t" with sigStamp.generationTime.toString()
                "k" with sigStamp.b64EncodedSignature
                "m" with "react"
                "tag" with tag
                "op" with operation
                "cid" with item.id
                "ck" with item.nonce
            }.body<MobileConfResult>().success
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
        val b64 = sigStamp.b64EncodedSignature.encodeURLParameter()
        return "https://steamcommunity.com/mobileconf/detailspage/${item.id}?p=${steamClient.configuration.getUuid()}&a=${instance.steamId.longId}&k=$b64&t=${sigStamp.generationTime}&m=react&tag=detail"
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit
}