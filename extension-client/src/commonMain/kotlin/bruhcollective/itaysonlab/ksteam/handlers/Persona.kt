package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EClientPersonaStateFlag
import bruhcollective.itaysonlab.ksteam.models.enums.EFriendRelationship
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EPersonaState
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.models.enums.relationship
import bruhcollective.itaysonlab.ksteam.models.enums.steamId
import bruhcollective.itaysonlab.ksteam.models.persona.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.persona.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import steam.webui.common.CMsgClientAccountInfo
import steam.webui.common.CMsgClientChangeStatus
import steam.webui.common.CMsgClientClanState
import steam.webui.common.CMsgClientLogonResponse
import steam.webui.common.CMsgClientPersonaState
import steam.webui.common.CMsgClientPersonaState_Friend
import steam.webui.common.CMsgClientRequestFriendData
import steam.webui.friendslist.CMsgClientFriendsList

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Persona internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    private val personas = MutableStateFlow<PersonaList>(emptyMap())

    private val _currentPersonaData = MutableStateFlow(CurrentPersona.Unknown)
    val currentPersona = _currentPersonaData.asStateFlow()

    private val _currentPersonaOnlineStatus = MutableStateFlow(EPersonaState.Offline)
    val currentPersonaOnlineStatus = _currentPersonaOnlineStatus.asStateFlow()

    private val _currentFriendList = MutableStateFlow<FriendsList>(emptyMap())
    val currentFriendList = _currentFriendList.asStateFlow()

    private val personaUpdateMutex = Mutex()
    private val personaFriendListUpdateMutex = Mutex()

    private suspend fun updatePersonaState(incoming: List<CMsgClientPersonaState_Friend>) = personaUpdateMutex.withLock {
        KSteamLogging.logVerbose("Persona:NewState") { "Incoming: ${incoming.joinToString()}" }

        personas.update { map ->
            map.toMutableMap().apply {
                incoming.forEach { friend ->
                    put(SteamId(friend.friendid?.toULong() ?: 0u), Persona(friend))
                }
            }
        }
    }

    /**
     * Provides live [Persona] data for several [SteamId]'s.
     *
     * This method also can return data for non-friends, but it is not known if updates can be received for them.
     */
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

    /**
     * Provides live [Persona] data for the specific [SteamId].
     *
     * This method also can return data for non-friends, but it is not known if updates can be received for them.
     */
    suspend fun persona(id: SteamId): Flow<Persona> {
        if (personas.value.containsKey(id).not()) {
            requestPersonas(listOf(id))
        }

        return personas.mapNotNull {
            it[id]
        }
    }

    /**
     * Sets current user's online status.
     *
     * Note, that [EPersonaState.Offline] will make you appear "offline", but kSteam won't be able to receive [Persona] updates.
     */
    suspend fun setOnlineStatus(mode: EPersonaState = EPersonaState.Online, stateFlags: Int = 0) {
        if (_currentPersonaOnlineStatus.value == mode) return
        _currentPersonaOnlineStatus.value = mode
        steamClient.executeAndForget(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientChangeStatus,
                adapter = CMsgClientChangeStatus.ADAPTER,
                payload = CMsgClientChangeStatus(
                    persona_state = mode.ordinal,
                    persona_state_flags = stateFlags
                )
            )
        )
    }

    /**
     * Returns a Flow with a current user [Persona] data.
     */
    fun currentLivePersona() = personas.combine(currentPersona) { personaMap, signedInPersona ->
        personaMap[signedInPersona.id] ?: Persona.Unknown
    }

    /**
     * Returns [SteamId]'s relationship with the current user.
     */
    fun personaRelationship(steamId: SteamId): Flow<EFriendRelationship> = currentFriendList.map {
        it[steamId] ?: EFriendRelationship.None
    }

    private suspend fun handleFriendListChanges(newList: CMsgClientFriendsList) = personaFriendListUpdateMutex.withLock {
        _currentFriendList.update {
            // 1. Request persona states
            requestPersonas(if (newList.bincremental == true) {
                newList.friends.filterNot { f -> f.relationship == EFriendRelationship.None }
                    .map { f -> f.steamId }
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

    /**
     * Preload specified [ids] into the cache.
     */
    suspend fun requestPersonas(ids: List<SteamId>) {
        if (ids.isEmpty()) return

        KSteamLogging.logDebug("Handlers:Persona") {
            "Requesting persona states for: ${ids.joinToString { it.id.toString() }}"
        }

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

            EMsg.k_EMsgClientClanState-> {
                KSteamLogging.logVerbose("Persona") {
                    packet.getProtoPayload(CMsgClientClanState.ADAPTER).dataNullable.toString()
                }
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