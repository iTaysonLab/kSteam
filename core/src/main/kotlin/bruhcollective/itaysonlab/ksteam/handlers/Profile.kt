package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.persona.*
import bruhcollective.itaysonlab.ksteam.models.persona.ProfileCustomization
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import steam.webui.player.*
import kotlin.time.Duration.Companion.minutes

/**
 * Access profile data using this interface.
 *
 * This differs from [Persona] handler:
 * - it can provide equipment data (equipped showcases or itemshop items)
 */
class Profile internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    private val _currentProfileEquipment = MutableStateFlow(ProfileEquipment())

    /**
     * Returns a Flow of your current equipment.
     */
    fun getMyEquipment() = _currentProfileEquipment.asStateFlow()

    /**
     * Returns equipped items of a specific user.
     *
     * Not recommended to use if [steamId] is referring to a current user. Use a [getMyEquipment] function to obtain live equipment information.
     */
    suspend fun getEquipment(steamId: SteamId): ProfileEquipment {
        return steamClient.webApi.execute(
            methodName = "Player.GetProfileItemsEquipped",
            requestAdapter = CPlayer_GetProfileItemsEquipped_Request.ADAPTER,
            responseAdapter = CPlayer_GetProfileItemsEquipped_Response.ADAPTER,
            requestData = CPlayer_GetProfileItemsEquipped_Request(steamid = steamId.longId, language = steamClient.config.language.vdfName)
        ).data.let { ProfileEquipment(it) }
    }

    /**
     * Returns customization of a specific user.
     */
    suspend fun getCustomization(steamId: SteamId, includePurchased: Boolean = false, includeInactive: Boolean = false): ProfileCustomization {
        return steamClient.webApi.execute(
            methodName = "Player.GetProfileCustomization",
            requestAdapter = CPlayer_GetProfileCustomization_Request.ADAPTER,
            responseAdapter = CPlayer_GetProfileCustomization_Response.ADAPTER,
            requestData = CPlayer_GetProfileCustomization_Request(steamid = steamId.longId, include_inactive_customizations = includeInactive, include_purchased_customizations = includePurchased)
        ).data.let { ProfileCustomization(it) }
    }

    /**
     * Gets a copy of [Persona], but using the external web API.
     *
     * This allows us to request more details (country/state) from friends or to get any data from non-friends.
     *
     * Data is returned as a [Flow] with a update rate of 5 minutes.
     */
    fun getProfiles(steamIds: List<SteamId>): Flow<List<SummaryPersona>> {
        return flow {
            while (true) {
                emit(
                    steamClient.externalWebApi.ajaxGetTyped<PlayerSummaries>(
                        baseUrl = EnvironmentConstants.WEB_API_BASE,
                        path = listOf("ISteamUserOAuth", "GetUserSummaries", "v2"),
                        parameters = mapOf(
                            "steamids" to steamIds.joinToString(",") { it.longId.toString() }
                        )
                    ).players.map(::SummaryPersona)
                )

                delay(5.minutes)
            }
        }
    }

    fun getProfile(steamId: SteamId) = getProfiles(listOf(steamId)).map(List<SummaryPersona>::first)

    suspend fun getProfilesNow(steamIds: List<SteamId>) = getProfiles(steamIds).first()
    suspend fun getProfileNow(steamId: SteamId) = getProfile(steamId).first()

    private suspend fun requestMyEquipment() {
        _currentProfileEquipment.update {
            getEquipment(steamClient.currentSessionSteamId)
        }
    }

    override suspend fun onEvent(packet: SteamPacket) {
        if (packet.messageId == EMsg.k_EMsgClientLogOnResponse) {
            requestMyEquipment()
        }
    }

    override suspend fun onRpcEvent(rpcMethod: String, packet: SteamPacket) {
        if (rpcMethod == "PlayerClient.NotifyFriendEquippedProfileItemsChanged#1") {
            val accountIdContainer = packet.getProtoPayload(CPlayer_FriendEquippedProfileItemsChanged_Notification.ADAPTER).dataNullable
            logDebug("Profile:RpcEvent", "Received NotifyFriendEquippedProfileItemsChanged, target account id: ${accountIdContainer?.accountid} [current account id: ${steamClient.currentSessionSteamId.accountId}]")
            if (accountIdContainer != null && accountIdContainer.accountid == steamClient.currentSessionSteamId.accountId) {
                requestMyEquipment()
            }
        }
    }
}