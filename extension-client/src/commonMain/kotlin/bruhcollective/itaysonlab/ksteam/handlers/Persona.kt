package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersona
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaRelationship
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.persona.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.persona.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.InitialResults
import io.realm.kotlin.notifications.PendingObject
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
    private val database: KSteamRealmDatabase
) {
    private val _currentPersonaData = MutableStateFlow(CurrentPersona.Unknown)
    val currentPersona = _currentPersonaData.asStateFlow()

    private val _currentPersonaOnlineStatus = MutableStateFlow(EPersonaState.Offline)
    val currentPersonaOnlineStatus = _currentPersonaOnlineStatus.asStateFlow()

    private suspend fun updatePersonaState(incoming: List<CMsgClientPersonaState_Friend>) {
        steamClient.logger.logVerbose("Persona") { "realm/persona@new ${incoming.joinToString()}" }

        database.realm.write {
            incoming.forEach { friend ->
                copyToRealm(RealmPersona(Persona(friend)), updatePolicy = UpdatePolicy.ALL)
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
                steamClient.logger.logVerbose("Persona") { "realm/mutiple@initial missing: [${missingSteamIds.joinToString()}]" }
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
                steamClient.logger.logVerbose("Persona") { "realm/single@pending $id" }
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
        return database.realm.query<RealmPersonaRelationship>("id == $0", "${currentPersona.value.id}_${steamId}")
            .first()
            .asFlow()
            .map { it.obj?.convert() ?: EFriendRelationship.None }
    }

    private suspend fun handleFriendListChanges(newList: CMsgClientFriendsList) {
        // 1. Request persona states
        requestPersonas(if (newList.bincremental == true) {
            newList.friends.filterNot { f -> f.relationship == EFriendRelationship.None }
                .map { f -> f.steamId }
        } else {
            newList.friends.map { f -> f.steamId }
        })

        // 2. Update friend-list flow
        database.realm.write {
            newList.friends.forEach { changedFriend ->
                steamClient.logger.logVerbose("Persona") { "realm/friends@change ${changedFriend.ulfriendid} -> ${EFriendRelationship.byEncoded(changedFriend.efriendrelationship)}" }
                copyToRealm(RealmPersonaRelationship(src = currentPersona.value.id, target = changedFriend.ulfriendid.toSteamId(), enum = changedFriend.efriendrelationship ?: 0), updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    /**
     * Preload specified [ids] into the cache.
     */
    suspend fun requestPersonas(ids: List<SteamId>) {
        if (ids.isEmpty()) return

        steamClient.logger.logDebug("Handlers:Persona") {
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

    init {
        steamClient.on(EMsg.k_EMsgClientPersonaState) { packet ->
            updatePersonaState(CMsgClientPersonaState.ADAPTER.decode(packet.payload).friends)
        }

        steamClient.on(EMsg.k_EMsgClientClanState) { packet ->
            steamClient.logger.logVerbose("Persona-ClanState") {
                CMsgClientClanState.ADAPTER.decode(packet.payload).toString()
            }
        }

        steamClient.on(EMsg.k_EMsgClientAccountInfo) { packet ->
            CMsgClientAccountInfo.ADAPTER.decode(packet.payload).let { obj ->
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