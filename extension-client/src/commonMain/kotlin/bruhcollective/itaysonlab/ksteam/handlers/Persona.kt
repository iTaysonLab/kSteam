package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRoomDatabase
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersona
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersonaRelationship
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersonaRpKvo
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.persona.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.persona.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import steam.webui.common.*
import steam.webui.friendslist.CMsgClientFriendsList

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Persona internal constructor(
    private val steamClient: ExtendedSteamClient,
    private val database: KSteamRoomDatabase
) {
    private companion object {
        private const val TAG = "PersonaHandler"
    }

    private val _currentPersonaData = MutableStateFlow(CurrentPersona.Unknown)
    val currentPersona = _currentPersonaData.asStateFlow()

    private val _currentPersonaOnlineStatus = MutableStateFlow(EPersonaState.Offline)
    val currentPersonaOnlineStatus = _currentPersonaOnlineStatus.asStateFlow()

    /**
     * Provides live [Persona] data for the specific [SteamId].
     *
     * If the data is not present in the database yet, the [Flow] will return a placeholder.
     * This method also can return data for non-friends, but it is not known if updates can be received for them.
     */
    fun persona(id: SteamId): Flow<Persona> {
        return database.currentUserDatabase.personas().getFullFlowById(id.longId).onEach { room ->
            if (room == null) {
                steamClient.logger.logVerbose(TAG) { "database miss: $id" }
                requestPersonas(listOf(id))
            }
        }.map { full ->
            full?.convert() ?: Persona.Unknown
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

        steamClient.execute(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientChangeStatus,
                payload = CMsgClientChangeStatus(
                    persona_state = mode.ordinal,
                    persona_state_flags = stateFlags
                )
            )
        )
    }

    /**
     * Returns a Flow with a current user [Persona] data.
     *
     * If the data is not present in the database yet, the [Flow] will return a placeholder.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun currentLivePersona(): Flow<Persona> = _currentPersonaData
        .distinctUntilChangedBy(CurrentPersona::id)
        .transform { currentPersona -> persona(currentPersona.id) }

    private suspend fun handleFriendListChanges(newList: CMsgClientFriendsList) {
        steamClient.logger.logVerbose(TAG) { "[handleFriendListChanges] $newList" }

        // 1. Request persona states
        requestPersonas(if (newList.bincremental == true) {
            newList.friends.filterNot { f -> f.relationship == EFriendRelationship.None }
                .map { f -> f.steamId }
        } else {
            newList.friends.map { f -> f.steamId }
        })

        // 2. Update friend-list flow
        database.currentUserDatabase.personas().updateRelationships(
            newList.friends.map { relationship ->
                RoomPersonaRelationship(uid = relationship.ulfriendid!!, relationship = relationship.efriendrelationship ?: 0)
            }
        )
    }

    /**
     * Preload specified [ids] into the cache.
     */
    suspend fun requestPersonas(ids: List<SteamId>) {
        if (ids.isEmpty()) return

        steamClient.logger.logDebug(TAG) { "[requestPersonas] ${ids.joinToString { it.id.toString() }}" }

        steamClient.execute(SteamPacket.newProto(
            messageId = EMsg.k_EMsgClientRequestFriendData,
            payload = CMsgClientRequestFriendData(
                persona_state_requested = EClientPersonaStateFlag.Default,
                friends = ids.map { it.id.toLong() }
            )
        ))
    }

    init {
        steamClient.on(EMsg.k_EMsgClientPersonaState) { packet ->
            val friends = CMsgClientPersonaState.ADAPTER.decode(packet.payload).friends

            database.currentUserDatabase.personas().upsertPersonaRpKvos(
                friends.filter {
                    it.rich_presence.isNotEmpty()
                }.flatMap { friend ->
                    friend.rich_presence.map { kv -> RoomPersonaRpKvo(uid = friend.friendid!!, key = kv.key!!, value = kv.value_!!) }
                }
            )

            database.currentUserDatabase.personas().upsertPersonas(
                persona = CMsgClientPersonaState.ADAPTER.decode(packet.payload).friends.map(RoomPersona::fromProtobufPersona)
            )
        }

        steamClient.on(EMsg.k_EMsgClientClanState) { packet ->
            steamClient.logger.logWarning("Persona-ClanState") {
                CMsgClientClanState.ADAPTER.decode(packet.payload).toString()
            }
        }

        steamClient.on(EMsg.k_EMsgClientAccountInfo) { packet ->
            CMsgClientAccountInfo.ADAPTER.decode(packet.payload).let { obj ->
                val steamId = SteamId(packet.header.steamId)

                _currentPersonaData.update {
                    it.copy(
                        id = steamId,
                        name = obj.persona_name.orEmpty(),
                        flags = AccountFlags(obj.account_flags ?: 0),
                        country = obj.ip_country ?: "US",
                    )
                }
            }

            setOnlineStatus(EPersonaState.Online)
        }

        steamClient.on(EMsg.k_EMsgClientFriendsList) { packet ->
            handleFriendListChanges(CMsgClientFriendsList.ADAPTER.decode(packet.payload))
        }

        steamClient.on(EMsg.k_EMsgClientLogOnResponse) { packet ->
            if (packet.isProtobuf()) {
                CMsgClientLogonResponse.ADAPTER.decode(packet.payload).let { logonResponse ->
                    if (logonResponse.eresult != EResult.OK.encoded) return@let

                    _currentPersonaData.update {
                        it.copy(vanityUrl = logonResponse.vanity_url.orEmpty())
                    }
                }
            }
        }
    }
}