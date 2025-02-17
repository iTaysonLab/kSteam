package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.guard.ConfirmationListState
import bruhcollective.itaysonlab.ksteam.models.guard.MobileConfResult
import bruhcollective.itaysonlab.ksteam.models.guard.MobileConfirmationItem
import bruhcollective.itaysonlab.ksteam.models.SteamId
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Mobile confirmations using a Steam Guard instance.
 */
class GuardConfirmation(
    private val steamClient: ExtendedSteamClient
) {
    /**
     * Get a list of confirmations waiting for the response.
     */
    suspend fun getConfirmations(steamId: SteamId): ConfirmationListState {
        val instance = steamClient.guard.instanceFor(steamId) ?: return ConfirmationListState.Error("kSteam does not have SG metadata for user ${steamId}")

        return try {
            instance.confirmationTicket("list").let { sigStamp ->
                steamClient.webApi.community.method("mobileconf/getlist") {
                    "p" with steamClient.configuration.getUuid()
                    "a" with steamId.longId
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
    suspend fun setConfirmationStatus(steamId: SteamId, item: MobileConfirmationItem, allow: Boolean): Boolean {
        val instance = steamClient.guard.instanceFor(steamId) ?: return false

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
                "a" with steamId.longId.toString()
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
        steamId: SteamId,
        item: MobileConfirmationItem
    ): String {
        val instance = steamClient.guard.instanceFor(steamId) ?: return ""

        val sigStamp = instance.confirmationTicket("detail")
        val b64 = sigStamp.b64EncodedSignature.encodeURLParameter()

        return "https://steamcommunity.com/mobileconf/detailspage/${item.id}?p=${steamClient.configuration.getUuid()}&a=${steamId.longId}&k=$b64&t=${sigStamp.generationTime}&m=react&tag=detail"
    }
}