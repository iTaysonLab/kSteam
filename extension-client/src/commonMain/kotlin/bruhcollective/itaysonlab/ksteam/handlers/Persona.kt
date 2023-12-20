package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersona
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.persona.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.persona.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.notifications.InitialResults
import io.realm.kotlin.notifications.PendingObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import steam.webui.common.*
import steam.webui.friendslist.CMsgClientFriendsList

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Persona internal constructor(
    private val steamClient: SteamClient,
    private val database: KSteamRealmDatabase
) : BaseHandler {
    private val _currentPersonaData = MutableStateFlow(CurrentPersona.Unknown)
    val currentPersona = _currentPersonaData.asStateFlow()

    private val _currentPersonaOnlineStatus = MutableStateFlow(EPersonaState.Offline)
    val currentPersonaOnlineStatus = _currentPersonaOnlineStatus.asStateFlow()

    private val personaFriendListUpdateMutex = Mutex()

    private suspend fun updatePersonaState(incoming: List<CMsgClientPersonaState_Friend>) {
        KSteamLogging.logVerbose("Persona") { "realm/persona@new ${incoming.joinToString()}" }

        database.realm.write {
            incoming.forEach { friend ->
                // TODO: research how we can save friendRelationshipsWith with a better codestyle
                val currentFriend = query<RealmPersona>("id == $0", friend.friendid).first().find()

                copyToRealm(RealmPersona(Persona(friend)).apply {
                    friendRelationshipsWith.putAll(currentFriend?.friendRelationshipsWith ?: realmDictionaryOf())
                }, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    /**
     * Provides live [Persona] data for several [SteamId]'s.
     * If the data for some [SteamId]'s are not present in the database yet, the [Flow] will not return them in the list.
     *
     * This method also can return data for non-friends, but it is not known if updates can be received for them.
     */
    suspend fun personas(ids: List<SteamId>): Flow<List<Persona>> {
        return database.realm.query<RealmPersona>(
            "id IN $0", ids.map(SteamId::longId)
        ).asFlow().onEach { realmChanges ->
            // Hook our InitialResults change result to quickly request for more personas
            if (realmChanges is InitialResults<RealmPersona>) {
                val missingSteamIds = ids - realmChanges.list.map { it.id.toSteamId() }.toSet()
                KSteamLogging.logVerbose("Persona") { "realm/mutiple@initial missing: [${missingSteamIds.joinToString()}]" }
                requestPersonas(missingSteamIds) // this will give us missing SteamID's
            }
        }.map { it.list.map(RealmPersona::convert) } // and this will give us kSteam Personas
    }

    /**
     * Provides live [Persona] data for the specific [SteamId].
     *
     * If the data is not present in the database yet, the [Flow] will return a placeholder.
     * This method also can return data for non-friends, but it is not known if updates can be received for them.
     */
    suspend fun persona(id: SteamId): Flow<Persona> {
        return database.realm.query<RealmPersona>(
            "id == $0", id.longId
        ).first().asFlow().onEach { realmChanges ->
            if (realmChanges is PendingObject<RealmPersona>) {
                KSteamLogging.logVerbose("Persona") { "realm/single@pending $id" }
                requestPersonas(listOf(id))
            }
        }.map { it.obj?.convert() ?: Persona.Unknown } // and this will give us kSteam Persona
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
     *
     * If the data is not present in the database yet, the [Flow] will return a placeholder.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun currentLivePersona(): Flow<Persona> = _currentPersonaData.distinctUntilChangedBy {
        it.id
    }.flatMapLatest { currentPersona ->
        database.realm.query<RealmPersona>(
            "id == $0", currentPersona.id.longId
        ).first().asFlow()
    }.map { it.obj?.convert() ?: Persona.Unknown }

    /**
     * Returns [SteamId]'s relationship with the current user.
     */
    fun personaRelationship(steamId: SteamId): Flow<EFriendRelationship> {
        return database.realm.query<RealmPersona>("id == $0", steamId.longId)
            .first()
            .asFlow()
            .map { EFriendRelationship.byEncoded(it.obj?.friendRelationshipsWith?.get(steamId.toString())) }
    }

    private suspend fun handleFriendListChanges(newList: CMsgClientFriendsList) {
        personaFriendListUpdateMutex.withLock {
            // 1. Request persona states
            requestPersonas(if (newList.bincremental == true) {
                newList.friends.filterNot { f -> f.relationship == EFriendRelationship.None }
                    .map { f -> f.steamId }
            } else {
                newList.friends.map { f -> f.steamId }
            })

            // 2. Update friend-list flow
            database.realm.write {
                val steamLongId = currentPersona.value.id.longId

                newList.friends.forEach { changedFriend ->
                    KSteamLogging.logVerbose("Persona") { "realm/friends@change ${changedFriend.ulfriendid} -> ${changedFriend.efriendrelationship}" }
                    val persona = query<RealmPersona>("id == $0", changedFriend.ulfriendid).first().find() ?: copyToRealm(RealmPersona().apply { id = changedFriend.ulfriendid ?: 0 })
                    persona.friendRelationshipsWith[steamLongId.toString()] = changedFriend.efriendrelationship ?: 0
                }
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

            EMsg.k_EMsgClientClanState -> {
                KSteamLogging.logVerbose("Persona-ClanState") {
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