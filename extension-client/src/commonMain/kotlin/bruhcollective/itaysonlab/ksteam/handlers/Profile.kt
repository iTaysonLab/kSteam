package bruhcollective.itaysonlab.ksteam.handlers

import androidx.collection.mutableScatterMapOf
import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.persona.*
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.persona.ProfileCustomization
import bruhcollective.itaysonlab.ksteam.models.persona.ProfilePreferences
import bruhcollective.itaysonlab.ksteam.models.persona.ProfileTheme
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import steam.enums.EProfileCustomizationType
import steam.webui.player.*

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
     * A flow of current signed-in user equipment.
     */
    val currentProfileEquipment: StateFlow<ProfileEquipment> = _currentProfileEquipment.asStateFlow()

    /**
     * Returns equipped items of a specific user.
     *
     * If [steamId] is referring to a current user, use [currentProfileEquipment] function to obtain live equipment information.
     */
    suspend fun getEquipment(steamId: SteamId): ProfileEquipment {
        if (steamId == steamClient.currentSessionSteamId) {
            return _currentProfileEquipment.value
        }

        return steamClient.grpc.player.GetProfileItemsEquipped().executeSteam(
            data = CPlayer_GetProfileItemsEquipped_Request(steamid = steamId.longId, language = steamClient.language.vdfName)
        ).let(::ProfileEquipment)
    }

    /**
     * Returns achievement progress in specific games of a user.
     *
     * **NOTE:** returns raw proto data until a replacement API is made.
     */
    suspend fun getAchievementsProgress(steamId: SteamId, appIds: List<AppId>): Map<Int, CPlayer_GetAchievementsProgress_Response_AchievementProgress> {
        return steamClient.grpc.player.GetAchievementsProgress().executeSteam(
            data = CPlayer_GetAchievementsProgress_Request(steamid = steamId.longId, language = steamClient.language.vdfName, appids = appIds.map(AppId::value))
        ).achievement_progress.associateBy { it.appid ?: 0 }
    }

    /**
     * Returns top achievements in a specific game.
     *
     * **NOTE:** returns raw proto data until a replacement API is made.
     */
    suspend fun getTopAchievements(steamId: SteamId, appIds: List<AppId>, count: Int = 5): Map<Int, List<CPlayer_GetTopAchievementsForGames_Response_Achievement>> {
        return steamClient.grpc.player.GetTopAchievementsForGames().executeSteam(
            data = CPlayer_GetTopAchievementsForGames_Request(steamid = steamId.longId, language = steamClient.language.vdfName, appids = appIds.map(AppId::value), max_achievements = count)
        ).games.associate { (it.appid ?: 0) to it.achievements }
    }

    /**
     * Returns customization of a specific user.
     *
     * This includes Points Shop data as well as widgets (not all are supported without scraping HTML)
     */
    suspend fun getCustomization(steamId: SteamId, includePurchased: Boolean = false, includeInactive: Boolean = false): ProfileCustomization {
        fun List<steam.webui.player.ProfileCustomization>.mapEntriesToAppIds() = this.map { it.slots.mapNotNull { s -> AppId(s.appid ?: return@mapNotNull null) } }
            .flatten()

        val customization = steamClient.grpc.player.GetProfileCustomization().executeSteam(
            data = CPlayer_GetProfileCustomization_Request(steamid = steamId.longId, include_inactive_customizations = includeInactive, include_purchased_customizations = includePurchased)
        )

        val appSummaries = customization.customizations
            .mapEntriesToAppIds()
            .let { steamClient.store.querySteamApplications(it) }
            .associateBy(SteamApplication::id)

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

        val ownedGames = if (customization.customizations.any {
            it.customization_type == EProfileCustomizationType.k_EProfileCustomizationTypeGameCollector.value ||
            it.customization_type == EProfileCustomizationType.k_EProfileCustomizationTypeFavoriteGame.value
        }) {
            runCatching {
                steamClient.player.getOwnedGames(steamId, includeFreeGames = true).associateBy { AppId(it.id) }
            }.getOrNull() ?: emptyMap()
        } else {
            emptyMap()
        }

        val widgets = customization.customizations.mapNotNull { protoWidget ->
            when (val enumType = EProfileCustomizationType.fromValue(protoWidget.customization_type ?: 0) ?: EProfileCustomizationType.k_EProfileCustomizationTypeInvalid) {
                EProfileCustomizationType.k_EProfileCustomizationTypeGameCollector -> {
                    ProfileWidget.GameCollector(
                        featuredApps = protoWidget.slots.mapNotNull { AppId(it.appid ?: return@mapNotNull null) }.mapNotNull { appSummaries[it] },
                        ownedGamesCount = ownedGames.size
                    )
                }

                EProfileCustomizationType.k_EProfileCustomizationTypeFavoriteGame -> {
                    val appId = AppId(protoWidget.slots.first().appid ?: return@mapNotNull null)

                    ProfileWidget.FavoriteGame(
                        app = appSummaries[appId] ?: return@mapNotNull null,
                        playedSeconds = ownedGames[appId]?.totalPlaytime ?: 0,
                        achievementProgress = achievements.second[appId.value]?.let { progress ->
                            ProfileWidget.FavoriteGame.AchievementProgress(
                                currentAchievements = progress.unlocked ?: 0,
                                totalAchievements = progress.total ?: 0,
                                topPictures = achievements.first[appId.value]?.sortedBy { it.player_percent_unlocked }?.map { EnvironmentConstants.formatCommunityImageUrl(appId.value, it.icon.orEmpty()) } ?: emptyList()
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
     * Queries profile summaries from the web API and returns a list of [Persona].
     *
     * This works on any Steam user.
     */
    suspend fun getProfileSummaries(steamIds: List<SteamId>): List<Persona> {
        if (steamIds.isEmpty()) {
            return emptyList() // short-circuit
        }

        return steamClient.webApi.gateway.method("ISteamUserOAuth/GetUserSummaries/v2") {
            "steamids" with steamIds.joinToString(",") { it.longId.toString() }
        }.body<PlayerSummaries>().players.map(Persona::fromSummary)
    }

    /**
     * Queries a profile summary from the web API and returns a [Persona].
     *
     * This works on any Steam user.
     */
    suspend fun getProfileSummary(steamId: SteamId) = getProfileSummaries(listOf(steamId)).first()

    /**
     * An optimized version of persona fetcher.
     *
     * Automatically returns data from local database if a user is a friend or somewhat related to current signed-in user.
     * Otherwise, requests the summaries from an online source.
     */
    suspend fun queryPersonas(ids: List<SteamId>): List<Persona> {
        val cache = mutableScatterMapOf<SteamId, Persona>()

        val (cached, notCached) = ids.partition { id ->
            steamClient.persona.localPersona(id)?.also { localPersona ->
                cache[id] = localPersona
            } != null
        }

        return cached.map { id ->
            cache.getOrElse(id, Persona::Unknown)
        } + getProfileSummaries(notCached)
    }

    private suspend fun requestMyEquipment() {
        runCatching {
            _currentProfileEquipment.update {
                getEquipment(steamClient.currentSessionSteamId)
            }
        }
    }

    init {
        steamClient.on(EMsg.k_EMsgClientLogOnResponse) {
            requestMyEquipment()
        }

        steamClient.onTypedRpc("PlayerClient.NotifyFriendEquippedProfileItemsChanged#1", CPlayer_FriendEquippedProfileItemsChanged_Notification.ADAPTER) { accountIdContainer ->
            steamClient.logger.logDebug("Profile:RpcEvent") { "Received NotifyFriendEquippedProfileItemsChanged, target account id: ${accountIdContainer.accountid} [current account id: ${steamClient.currentSessionSteamId.accountId}]" }

            if (accountIdContainer.accountid == steamClient.currentSessionSteamId.accountId) {
                requestMyEquipment()
            }
        }
    }
}