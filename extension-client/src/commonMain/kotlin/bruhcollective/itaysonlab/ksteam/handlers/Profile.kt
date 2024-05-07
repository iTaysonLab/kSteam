package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.persona.*
import bruhcollective.itaysonlab.ksteam.models.persona.ProfileCustomization
import bruhcollective.itaysonlab.ksteam.models.persona.ProfilePreferences
import bruhcollective.itaysonlab.ksteam.models.persona.ProfileTheme
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import steam.enums.EProfileCustomizationType
import steam.webui.player.*
import kotlin.time.Duration.Companion.minutes

/**
 * Access profile data using this interface.
 *
 * This differs from [Persona] handler:
 * - it can provide equipment data (equipped showcases or itemshop items)
 */
class Profile internal constructor(
    private val steamClient: ExtendedSteamClient
) {
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
        return steamClient.grpc.player.GetProfileItemsEquipped().executeSteam(
            data = CPlayer_GetProfileItemsEquipped_Request(steamid = steamId.longId, language = steamClient.language.vdfName)
        ).let(::ProfileEquipment)
    }

    /**
     * Returns achievement progress in specific games of a user.
     *
     * **NOTE:** returns raw proto data until a replacement API is made.
     */
    suspend fun getAchievementsProgress(steamId: SteamId, appIds: List<Int>): Map<Int, CPlayer_GetAchievementsProgress_Response_AchievementProgress> {
        return steamClient.grpc.player.GetAchievementsProgress().executeSteam(
            data = CPlayer_GetAchievementsProgress_Request(steamid = steamId.longId, language = steamClient.language.vdfName, appids = appIds)
        ).achievement_progress.associateBy { it.appid ?: 0 }
    }

    /**
     * Returns top achievements in a specific game.
     *
     * **NOTE:** returns raw proto data until a replacement API is made.
     */
    suspend fun getTopAchievements(steamId: SteamId, appIds: List<Int>, count: Int = 5): Map<Int, List<CPlayer_GetTopAchievementsForGames_Response_Achievement>> {
        return steamClient.grpc.player.GetTopAchievementsForGames().executeSteam(
            data = CPlayer_GetTopAchievementsForGames_Request(steamid = steamId.longId, language = steamClient.language.vdfName, appids = appIds, max_achievements = count)
        ).games.associate { (it.appid ?: 0) to it.achievements }
    }

    /**
     * Returns customization of a specific user.
     *
     * This includes Points Shop data as well as widgets (not all are supported without scraping HTML)
     */
    suspend fun getCustomization(steamId: SteamId, includePurchased: Boolean = false, includeInactive: Boolean = false): ProfileCustomization {
        fun List<steam.webui.player.ProfileCustomization>.mapEntriesToAppIds() = this.map { it.slots.mapNotNull { s -> s.appid } }
            .flatten()

        val ownedGames = steamClient.player.getOwnedGames(steamId, includeFreeGames = true).associateBy { it.id }

        val customization = steamClient.grpc.player.GetProfileCustomization().executeSteam(
            data = CPlayer_GetProfileCustomization_Request(steamid = steamId.longId, include_inactive_customizations = includeInactive, include_purchased_customizations = includePurchased)
        )

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
                        featuredApps = protoWidget.slots.mapNotNull { it.appid }.mapNotNull { appSummaries[it] },
                        ownedGamesCount = ownedGames.size
                    )
                }

                EProfileCustomizationType.k_EProfileCustomizationTypeFavoriteGame -> {
                    val appId = protoWidget.slots.first().appid ?: return@mapNotNull null

                    ProfileWidget.FavoriteGame(
                        app = appSummaries[appId] ?: return@mapNotNull null,
                        playedSeconds = ownedGames[appId]?.totalPlaytime ?: 0,
                        achievementProgress = achievements.second[appId]?.let { progress ->
                            ProfileWidget.FavoriteGame.AchievementProgress(
                                currentAchievements = progress.unlocked ?: 0,
                                totalAchievements = progress.total ?: 0,
                                topPictures = achievements.first[appId]?.sortedBy { it.player_percent_unlocked }?.map { EnvironmentConstants.formatCommunityImageUrl(appId, it.icon.orEmpty()) } ?: emptyList()
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
    fun getProfileSummariesAsFlow(steamIds: List<SteamId>): Flow<List<SummaryPersona>> {
        return flow {
            while (true) {
                emit(getProfileSummaries(steamIds))
                delay(5.minutes)
            }
        }
    }

    fun getProfileSummaryAsFlow(steamId: SteamId) = getProfileSummariesAsFlow(listOf(steamId)).map(List<SummaryPersona>::first)

    suspend fun getProfileSummaries(steamIds: List<SteamId>): List<SummaryPersona> {
        if (steamIds.isEmpty()) {
            return emptyList() // short-circuit
        }

        return steamClient.webApi.gateway.method("ISteamUserOAuth/GetUserSummaries/v2") {
            "steamids" with steamIds.joinToString(",") { it.longId.toString() }
        }.body<PlayerSummaries>().players.map(::SummaryPersona)
    }

    suspend fun getProfileSummary(steamId: SteamId) = getProfileSummaries(listOf(steamId)).first()

    private suspend fun requestMyEquipment() {
        _currentProfileEquipment.update {
            getEquipment(steamClient.currentSessionSteamId)
        }
    }

    init {
        steamClient.on(EMsg.k_EMsgClientLogOnResponse) {
            requestMyEquipment()
        }

        steamClient.onRpc("PlayerClient.NotifyFriendEquippedProfileItemsChanged#1") { packet ->
            val accountIdContainer = CPlayer_FriendEquippedProfileItemsChanged_Notification.ADAPTER.decode(packet.payload)

            steamClient.logger.logDebug("Profile:RpcEvent") { "Received NotifyFriendEquippedProfileItemsChanged, target account id: ${accountIdContainer?.accountid} [current account id: ${steamClient.currentSessionSteamId.accountId}]" }

            if (accountIdContainer.accountid == steamClient.currentSessionSteamId.accountId) {
                requestMyEquipment()
            }
        }
    }
}