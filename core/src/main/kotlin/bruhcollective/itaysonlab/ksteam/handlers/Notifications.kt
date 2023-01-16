package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Notifications(
    private val steamClient: SteamClient
): BaseHandler {
    private val webApi get() = steamClient.getHandler<WebApi>()

    private val _notifications = MutableStateFlow(mutableMapOf<SteamId, Persona>())
    private val notifications = _notifications.asStateFlow()

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            else -> {}
        }
    }
}