package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EClientPersonaStateFlag
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import kotlinx.coroutines.flow.*
import steam.messages.clientserver_friends.CMsgClientPersonaState
import steam.messages.clientserver_friends.CMsgClientRequestFriendData

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

    private fun updatePersonaState(incoming: List<CMsgClientPersonaState.Friend>) {
        personas.update { map ->
            map.apply {
                incoming.forEach { friend ->
                    put(SteamId(friend.friendid?.toULong() ?: 0u), Persona(friend))
                }
            }
        }
    }

    suspend fun personas(ids: List<SteamId>): Flow<List<Persona>> {
        ids.filterNot { personas.value.containsKey(it) }.let { nonPresentPersonas ->
            if (nonPresentPersonas.isNotEmpty()) {
                requestPersonas(nonPresentPersonas)
            }
        }

        return personas.map { map ->
            ids.mapNotNull { id -> map[id] }
        }
    }

    // TODO: maybe provide a default persona?
    suspend fun persona(id: SteamId): Flow<Persona> {
        if (personas.value.containsKey(id).not()) {
            requestPersonas(listOf(id))
        }

        return personas.mapNotNull {
            it[id]
        }
    }

    private suspend fun requestPersonas(ids: List<SteamId>) {
        steamClient.executeAndForget(SteamPacket.newProto(
            messageId = EMsg.k_EMsgClientRequestFriendData,
            adapter = CMsgClientRequestFriendData.ADAPTER,
            payload = CMsgClientRequestFriendData(
                persona_state_requested = EClientPersonaStateFlag.Default,
                friends = ids.map { it.id.toLong() }
            )
        ))
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            else -> {}
        }
    }
}