package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersona
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.persona.AccountFlags
import bruhcollective.itaysonlab.ksteam.models.persona.CurrentPersona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import bruhcollective.itaysonlab.ksteam.util.RichPresenceFormatter
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
    private val richPresenceFormatter = RichPresenceFormatter(steamClient, database)

    private val _currentPersonaData = MutableStateFlow(CurrentPersona.Unknown)
    val currentPersona = _currentPersonaData.asStateFlow()

    private val _currentPersonaOnlineStatus = MutableStateFlow(EPersonaState.Offline)
    val currentPersonaOnlineStatus = _currentPersonaOnlineStatus.asStateFlow()

    private suspend fun updatePersonaState(friend: CMsgClientPersonaState_Friend) {
        steamClient.logger.logVerbose("Persona") { "realm/persona@update $friend" }

        val ksPersona = Persona(friend)
        val ksPersonaStatus = ksPersona.status

        /*val inSteamDisplayText = if (ksPersonaStatus is Persona.Status.InGame) {
            richPresenceFormatter.formatRichPresenceText(
                appid = ksPersonaStatus.appId,
                language = steamClient.language,
                presence = ksPersonaStatus.richPresence
            )
        } else {
            null
        }*/

        database.currentUserRealm.write {
            val existingManagedRealmPersona = query<RealmPersona>("id == $0", ksPersona.id.longId).first().find()

            if (existingManagedRealmPersona != null) {
                existingManagedRealmPersona.merge(ksPersona)
            } else {
                copyToRealm(RealmPersona(ksPersona))
            }
        }

        if (ksPersona.status is Persona.Status.InGame) {
            // Dispatch
        }
    }

    /**
     * Provides live [Persona] data for several [SteamId]'s.
     * If the data for some [SteamId]'s are not present in the database yet, the [Flow] will not return them in the list.
     *
     * This method also can return data for non-friends, but it is not known if updates can be received for them.
     */
    fun personas(ids: List<SteamId>): Flow<List<Persona>> {
        return database.currentUserRealm.query<RealmPersona>(
            "id IN $0", ids.map(SteamId::longId)
        ).asFlow().onEach { realmChanges ->
            // Hook our InitialResults change result to quickly request for more personas
            if (realmChanges is InitialResults<RealmPersona>) {
                val missingSteamIds = ids - realmChanges.list.map { it.id.toSteamId() }.toSet()
                steamClient.logger.logVerbose("Persona") { "realm/mutiple@pending [${missingSteamIds.joinToString()}]" }
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
    fun persona(id: SteamId): Flow<Persona> {
        return database.currentUserRealm.query<RealmPersona>(
            "id == $0", id.longId
        ).first().asFlow().onEach { realmChanges ->
            if (realmChanges is PendingObject<RealmPersona>) {
                steamClient.logger.logVerbose("Persona") { "realm/single@pending [$id]" }
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
    fun currentLivePersona(): Flow<Persona> = _currentPersonaData.distinctUntilChangedBy {
        it.id
    }.flatMapLatest { currentPersona ->
        database.currentUserRealm.query<RealmPersona>(
            "id == $0", currentPersona.id.longId
        ).first().asFlow()
    }.map { it.obj?.convert() ?: Persona.Unknown }

    private suspend fun handleFriendListChanges(newList: CMsgClientFriendsList) {
        // 1. Request persona states
        requestPersonas(if (newList.bincremental == true) {
            newList.friends.filterNot { f -> f.relationship == EFriendRelationship.None }
                .map { f -> f.steamId }
        } else {
            newList.friends.map { f -> f.steamId }
        })

        // 2. Update friend-list flow
        database.currentUserRealm.write {
            for (relationship in newList.friends) {
                steamClient.logger.logVerbose("Persona") {
                    "realm/friends@updateRelationship ${relationship.ulfriendid} -> ${EFriendRelationship.byEncoded(relationship.efriendrelationship)}"
                }

                val existingManagedRealmPersona = query<RealmPersona>("id == $0", relationship.ulfriendid).first().find()

                if (existingManagedRealmPersona != null) {
                    existingManagedRealmPersona.relationship = relationship.efriendrelationship ?: 0
                } else {
                    val newRealmPersona = RealmPersona()
                    newRealmPersona.id = relationship.ulfriendid ?: return@write
                    newRealmPersona.relationship = relationship.efriendrelationship ?: 0
                    copyToRealm(newRealmPersona)
                }
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
            for (friend in CMsgClientPersonaState.ADAPTER.decode(packet.payload).friends) {
                updatePersonaState(friend)
            }
        }

        steamClient.on(EMsg.k_EMsgClientClanState) { packet ->
            steamClient.logger.logVerbose("Persona-ClanState") {
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