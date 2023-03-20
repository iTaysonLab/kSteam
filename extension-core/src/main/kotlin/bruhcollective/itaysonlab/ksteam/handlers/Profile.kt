package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.cdn.CommunityAppImageUrl
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.persona.*
import bruhcollective.itaysonlab.ksteam.models.persona.ProfileCustomization
import bruhcollective.itaysonlab.ksteam.models.persona.ProfilePreferences
import bruhcollective.itaysonlab.ksteam.models.persona.ProfileTheme
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
        return steamClient.unifiedMessages.execute(
            methodName = "Player.GetProfileItemsEquipped",
            requestAdapter = CPlayer_GetProfileItemsEquipped_Request.ADAPTER,
            responseAdapter = CPlayer_GetProfileItemsEquipped_Response.ADAPTER,
            requestData = CPlayer_GetProfileItemsEquipped_Request(steamid = steamId.longId, language = steamClient.language.vdfName)
        ).data.let { ProfileEquipment(it) }
    }

    private suspend fun getAchievementsProgress(steamId: SteamId, appIds: List<AppId>): Map<AppId, CPlayer_GetAchievementsProgress_Response_AchievementProgress> {
        return steamClient.unifiedMessages.execute(
            methodName = "Player.GetAchievementsProgress",
            requestAdapter = CPlayer_GetAchievementsProgress_Request.ADAPTER,
            responseAdapter = CPlayer_GetAchievementsProgress_Response.ADAPTER,
            requestData = CPlayer_GetAchievementsProgress_Request(steamid = steamId.longId, language = steamClient.language.vdfName, appids = appIds.map(AppId::id))
        ).data.achievement_progress.associateBy { AppId(it.appid ?: 0) }
    }

    private suspend fun getTopAchievements(steamId: SteamId, appIds: List<AppId>, count: Int = 5): Map<AppId, List<CPlayer_GetTopAchievementsForGames_Response_Achievement>> {
        return steamClient.unifiedMessages.execute(
            methodName = "Player.GetTopAchievementsForGames",
            requestAdapter = CPlayer_GetTopAchievementsForGames_Request.ADAPTER,
            responseAdapter = CPlayer_GetTopAchievementsForGames_Response.ADAPTER,
            requestData = CPlayer_GetTopAchievementsForGames_Request(steamid = steamId.longId, language = steamClient.language.vdfName, appids = appIds.map(AppId::id), max_achievements = count)
        ).data.games.associate { AppId(it.appid ?: 0) to it.achievements }
    }

    /**
     * Returns customization of a specific user.
     */
    suspend fun getCustomization(steamId: SteamId, includePurchased: Boolean = false, includeInactive: Boolean = false): ProfileCustomization {
        fun List<steam.webui.player.ProfileCustomization>.mapEntriesToAppIds() = this.map { it.slots.mapNotNull { s -> s.appid } }
            .flatten()
            .map(::AppId)

        val ownedGames = steamClient.player.getOwnedGames(steamId, includeFreeGames = true).associateBy { it.id }

        val customization = steamClient.unifiedMessages.execute(
            methodName = "Player.GetProfileCustomization",
            requestAdapter = CPlayer_GetProfileCustomization_Request.ADAPTER,
            responseAdapter = CPlayer_GetProfileCustomization_Response.ADAPTER,
            requestData = CPlayer_GetProfileCustomization_Request(steamid = steamId.longId, include_inactive_customizations = includeInactive, include_purchased_customizations = includePurchased)
        ).data

        val appSummaries = customization.customizations
            .mapEntriesToAppIds()
            .let { steamClient.store.getAppSummaries(it) }

        val achievements = customization.customizations
            .filter {
                (EProfileCustomizationType.fromValue(it.customization_type ?: 0) ?: EProfileCustomizationType.k_EProfileCustomizationTypeInvalid) in listOf(
                    EProfileCustomizationType.k_EProfileCustomizationTypeAchievements,
                    EProfileCustomizationType.k_EProfileCustomizationTypeAchievementsCompletionist,
                    EProfileCustomizationType.k_EProfileCustomizationTypeRareAchievementShowcase,
                    EProfileCustomizationType.k_EProfileCustomizationTypeFavoriteGame,
                )
            }
            .mapEntriesToAppIds()
            .let { getTopAchievements(steamId, it, count = 5) to getAchievementsProgress(steamId, it) }

        val widgets = customization.customizations.mapNotNull { protoWidget ->
            when (val enumType = EProfileCustomizationType.fromValue(protoWidget.customization_type ?: 0) ?: EProfileCustomizationType.k_EProfileCustomizationTypeInvalid) {
                EProfileCustomizationType.k_EProfileCustomizationTypeGameCollector -> {
                    ProfileWidget.GameCollector(
                        featuredApps = protoWidget.slots.mapNotNull { it.appid }.mapNotNull { appSummaries[AppId(it)] },
                        ownedGamesCount = ownedGames.size
                    )
                }

                EProfileCustomizationType.k_EProfileCustomizationTypeFavoriteGame -> {
                    val appId = AppId(protoWidget.slots.first().appid ?: return@mapNotNull null)

                    ProfileWidget.FavoriteGame(
                        app = appSummaries[appId] ?: return@mapNotNull null,
                        playedSeconds = ownedGames[appId]?.totalPlaytime ?: 0,
                        achievementProgress = achievements.second[appId]?.let { progress ->
                            ProfileWidget.FavoriteGame.AchievementProgress(
                                currentAchivements = progress.unlocked ?: 0,
                                totalAchievements = progress.total ?: 0,
                                topPictures = achievements.first[appId]?.sortedBy { it.player_percent_unlocked }?.map { CommunityAppImageUrl(appId.id to it.icon.orEmpty()) } ?: emptyList()
                            )
                        } ?: return@mapNotNull null
                    )
                }

                else -> {
                    ProfileWidget.Unknown(enumType)
                }
            }
        }

        return ProfileCustomization(
            profileWidgets = widgets,
            slotsAvailable = customization.slots_available ?: 0,
            profileTheme = customization.profile_theme?.let { ProfileTheme(it) },
            profilePreferences = customization.profile_preferences?.let { ProfilePreferences(it) }
        )
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
                    steamClient.webApi.ajaxGetTyped<PlayerSummaries>(
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
            KSteamLogging.logDebug("Profile:RpcEvent", "Received NotifyFriendEquippedProfileItemsChanged, target account id: ${accountIdContainer?.accountid} [current account id: ${steamClient.currentSessionSteamId.accountId}]")
            if (accountIdContainer != null && accountIdContainer.accountid == steamClient.currentSessionSteamId.accountId) {
                requestMyEquipment()
            }
        }
    }
}