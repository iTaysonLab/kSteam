package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.Persona
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import steam.messages.clientserver_friends.CMsgClientPersonaState

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Persona(
    private val steamClient: SteamClient
): BaseHandler {
    private val personas = MutableStateFlow(mutableMapOf<SteamId, Persona>())

    private val personaMap = personas.map {

    }

    private fun updatePersonaState(incoming: CMsgClientPersonaState.Friend) {
        personas.update { map ->
            map.apply {
                put(SteamId(incoming.friendid?.toULong() ?: 0u), bruhcollective.itaysonlab.ksteam.models.Persona(incoming))
            }
        }
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientPersonaState -> {
                packet.getProtoPayload(CMsgClientPersonaState.ADAPTER).data.friends.onEach(::updatePersonaState)
            }

            else -> {}
        }
    }
}