package bruhcollective.itaysonlab.ksteam.handlers

import CMsgClientFriendsList
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.persona.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.persona.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import steam.webui.common.*

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

    private val _currentPersonaOnlineStatus = MutableStateFlow(EPersonaState.Offline)
    val currentPersonaOnlineStatus = _currentPersonaOnlineStatus.asStateFlow()

    private fun updatePersonaState(incoming: List<CMsgClientPersonaState_Friend>) {
        logVerbose("Persona::NewState", "Incoming: ${incoming.joinToString()}")

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

    suspend fun setOnlineStatus(mode: EPersonaState = EPersonaState.Online) {
        if (_currentPersonaOnlineStatus.value == mode) return
        _currentPersonaOnlineStatus.value = mode
        steamClient.executeAndForget(SteamPacket.newProto(
            messageId = EMsg.k_EMsgClientChangeStatus,
            adapter = CMsgClientChangeStatus.ADAPTER,
            payload = CMsgClientChangeStatus(persona_state = mode.ordinal, persona_state_flags = if (steamClient.config.deviceInfo.gamingDeviceType == EGamingDeviceType.k_EGamingDeviceType_Phone) {
                EPersonaStateFlag.ClientTypeMobile.mask
            } else {
                0
            })
        ))
    }

    /**
     * Returns a Flow with a current user mapped to a [Persona].
     */
    fun currentLivePersona() = personas.combine(currentPersona) { personaMap, signedInPersona ->
        personaMap[signedInPersona.id] ?: Persona.Unknown
    }

    suspend fun requestPersonas(ids: List<SteamId>) {
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
                    requestPersonas(obj.friends.mapNotNull { it.ulfriendid }.map { SteamId(it.toULong()) })
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