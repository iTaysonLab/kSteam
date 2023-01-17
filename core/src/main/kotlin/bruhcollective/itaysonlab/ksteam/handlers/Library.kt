package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.apps.App
import bruhcollective.itaysonlab.ksteam.models.econ.EconItemReference
import bruhcollective.itaysonlab.ksteam.models.notifications.Notification
import bruhcollective.itaysonlab.ksteam.models.notifications.NotificationFeed
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.util.Cdn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import steam.webui.common.*
import steam.webui.steamnotification.CSteamNotification_GetSteamNotifications_Request
import steam.webui.steamnotification.CSteamNotification_GetSteamNotifications_Response
import steam.webui.steamnotification.SteamNotificationType

/**
 * Provides access to user's owned app library.
 */
class Library(
    private val steamClient: SteamClient
): BaseHandler {
    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            else -> {}
        }
    }
}