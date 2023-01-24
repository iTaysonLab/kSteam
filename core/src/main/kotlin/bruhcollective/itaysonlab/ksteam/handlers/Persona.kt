package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.persona.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.persona.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import kotlinx.coroutines.flow.*
import steam.webui.common.*
import steam.webui.friendslist.CMsgClientFriendsList

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Persona(
    private val steamClient: SteamClient
) : BaseHandler {
    private val personas = MutableStateFlow<PersonaList>(emptyMap())

    private val _currentPersonaData = MutableStateFlow(CurrentPersona.Unknown)
    val currentPersona = _currentPersonaData.asStateFlow()

    private val _currentPersonaOnlineStatus = MutableStateFlow(EPersonaState.Offline)
    val currentPersonaOnlineStatus = _currentPersonaOnlineStatus.asStateFlow()

    private val _currentFriendList = MutableStateFlow<FriendsList>(emptyMap())
    val currentFriendList = _currentFriendList.asStateFlow()

    private fun updatePersonaState(incoming: List<CMsgClientPersonaState_Friend>) {
        logVerbose("Persona::NewState", "Incoming: ${incoming.joinToString()}")

        personas.update { map ->
            map.toMutableMap().apply {
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

    suspend fun setOnlineStatus(mode: EPersonaState = EPersonaState.Online) {
        if (_currentPersonaOnlineStatus.value == mode) return
        _currentPersonaOnlineStatus.value = mode
        steamClient.executeAndForget(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientChangeStatus,
                adapter = CMsgClientChangeStatus.ADAPTER,
                payload = CMsgClientChangeStatus(
                    persona_state = mode.ordinal,
                    persona_state_flags = if (steamClient.config.deviceInfo.gamingDeviceType == EGamingDeviceType.k_EGamingDeviceType_Phone) {
                        EPersonaStateFlag.ClientTypeMobile.mask
                    } else {
                        0
                    }
                )
            )
        )
    }

    /**
     * Returns a Flow with a current user mapped to a [Persona].
     */
    fun currentLivePersona() = personas.combine(currentPersona) { personaMap, signedInPersona ->
        personaMap[signedInPersona.id] ?: Persona.Unknown
    }

    private suspend fun handleFriendListChanges(newList: CMsgClientFriendsList) {
        _currentFriendList.update {
            // 1. Request persona states
            requestPersonas(if (newList.bincremental == true) {
                newList.friends.filterNot { f -> f.relationship == EFriendRelationship.None }.map { f -> f.steamId }
            } else {
                newList.friends.map { f -> f.steamId }
            })

            // 2. Update friend-list flow
            if (newList.bincremental == true) {
                val parted = newList.friends.partition { friend ->
                    friend.relationship == EFriendRelationship.None
                }

                val removedIds = parted.first.map { f -> f.steamId }

                it.filterNot { f -> f.key in removedIds } + parted.second.associate { f -> f.steamId to f.relationship }
            } else {
                newList.friends.associate { f -> f.steamId to f.relationship }
            }
        }
    }

    suspend fun requestPersonas(ids: List<SteamId>) {
        if (ids.isEmpty()) return

        logDebug("Handlers::Persona", "Requesting persona states for: ${ids.joinToString { it.id.toString() }}")

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

                setOnlineStatus(EPersonaState.Online)
            }

            EMsg.k_EMsgClientFriendsList -> {
                packet.getProtoPayload(CMsgClientFriendsList.ADAPTER).data.let { obj ->
                    handleFriendListChanges(obj)
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

typealias PersonaList = Map<SteamId, Persona>
typealias FriendsList = Map<SteamId, EFriendRelationship>