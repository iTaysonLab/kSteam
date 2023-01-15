package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.Persona
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EClientPersonaStateFlag
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import kotlinx.coroutines.flow.*
import steam.messages.clientserver_friends.CMsgClientPersonaState
import steam.messages.clientserver_friends.CMsgClientRequestFriendData
import steam.messages.clientserver_login.CMsgClientAccountInfo
import steam.messages.clientserver_login.CMsgClientLogonResponse

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Persona(
    private val steamClient: SteamClient
): BaseHandler {
    private val personas = MutableStateFlow(mutableMapOf<SteamId, Persona>())

    private val _currentPersonaData = MutableStateFlow(CurrentPersona.Unknown)
    val currentPersona = _currentPersonaData.asStateFlow()

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
            EMsg.k_EMsgClientPersonaState -> {
                updatePersonaState(packet.getProtoPayload(CMsgClientPersonaState.ADAPTER).data.friends)
            }

            EMsg.k_EMsgClientAccountInfo -> {
                packet.getProtoPayload(CMsgClientAccountInfo.ADAPTER).data.let { obj ->
                    _currentPersonaData.update {
                        it.copy(
                            id = SteamId(packet.header.steamId),
                            name = obj.persona_name.orEmpty(),
                            flags = AccountFlags(obj.account_flags ?: 0),
                            country = obj.ip_country ?: "US",
                        )
                    }
                }
            }

            EMsg.k_EMsgClientLogOnResponse -> {
                packet.getProtoPayload(CMsgClientLogonResponse.ADAPTER).data.let { logonResponse ->
                    if (logonResponse.eresult != EResult.OK.encoded) return
                    _currentPersonaData.update {
                        it.copy(vanityUrl = logonResponse.vanity_url.orEmpty())
                    }
                }
            }

            else -> {}
        }
    }
}