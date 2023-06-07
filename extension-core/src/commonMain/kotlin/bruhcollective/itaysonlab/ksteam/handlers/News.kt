package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.cdn.CommunityAppImageUrl
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.enums.plus
import bruhcollective.itaysonlab.ksteam.models.news.AppType
import bruhcollective.itaysonlab.ksteam.models.news.EventType
import bruhcollective.itaysonlab.ksteam.models.news.NewsCalendarResponse
import bruhcollective.itaysonlab.ksteam.models.news.NewsEntry
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubResponse
import bruhcollective.itaysonlab.ksteam.models.news.usernews.ActivityFeedEntry
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import kotlinx.coroutines.flow.first
import steam.webui.usernews.CUserNews_GetUserNews_Request
import steam.webui.usernews.CUserNews_GetUserNews_Response

/**
 * Access Steam news using this handler.
 */
class News internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    /**
     * Get events in the given calendar range.
     *
     * @param range between two unix timestamps
     * @param collectionId the collection ID for news. For example, it could be "steam" for the Global News -> Steam Official section of the official news app.
     * @param count count of news to return
     * @param ascending sort from the earliest event
     * @param eventTypes show only chosen event types
     * @param appTypes show only chosen app types
     * @param filterByAppIds get data only for specific [AppId]s
     * @param filterByClanIds get data only for specific [SteamId] with a "clan" state
     *
     * @return a well-formed and parsed page ready to be shown in "News" screen
     */
    suspend fun getEventsInCalendarRange(
        range: LongRange,
        collectionId: String? = null,
        count: Int = 250,
        ascending: Boolean = false,
        eventTypes: List<EventType> = EventType.values().toList(),
        appTypes: List<AppType> = AppType.values().toList(),
        filterByAppIds: List<AppId> = emptyList(),
        filterByClanIds: List<SteamId> = emptyList(),
    ) {
        steamClient.webApi.ajaxGetTyped<NewsCalendarResponse>(path = listOf("events", "ajaxgetusereventcalendarrange", ""), parameters = mapOf(

        ))
    }

    /**
     * Get community hub - UGC content related to a specific game, such as photos/guides/videos/reviews.
     *
     * @param appId
     */
    suspend fun getCommunityHub(
        appId: AppId
    ): List<CommunityHubPost> {
        return steamClient.webApi.ajaxGetTyped<CommunityHubResponse>(
            path = listOf("library", "appcommunityfeed", appId.id.toString()),
            parameters = mapOf(
                "p" to "1",
                "filterLanguage" to steamClient.language.vdfName,
                "languageTag" to steamClient.language.vdfName,
                "nMaxInappropriateScore" to "1"
            ),
            repeatingParameters = mapOf("rgSections[]" to listOf("2", "3", "4", "9"))
        ).hub
    }

    /**
     * Get activity events for a specific [AppId].
     *
     * @param appId filter events for the specific app, set it to zero to show everything
     * @param showEvents filter events by their type, use [UserNewsFilterScenario] for ready-made presets
     * @param count limit the size of events
     */
    suspend fun getUserNews(
        appId: AppId = AppId(0),
        showEvents: Int = UserNewsFilterScenario.AppOverviewList,
        count: Int = 100,
        startTime: Int = 0,
        endTime: Int = 0,
    ): List<ActivityFeedEntry> {
        val newsProto = steamClient.unifiedMessages.execute(
            methodName = "UserNews.GetUserNews",
            requestAdapter = CUserNews_GetUserNews_Request.ADAPTER,
            responseAdapter = CUserNews_GetUserNews_Response.ADAPTER,
            requestData = CUserNews_GetUserNews_Request(
                filterappid = appId.id,
                filterflags = showEvents,
                count = count,
                starttime = startTime,
                endtime = endTime,
                language = steamClient.language.vdfName
            )
        ).data

        val achievementMap = newsProto.achievement_display_data.associate { displayData ->
            AppId(displayData.appid ?: 0) to displayData.achievements.associate { achievement ->
                achievement.name.orEmpty() to ActivityFeedEntry.NewAchievements.Achievement(
                    internalName = achievement.name.orEmpty(),
                    displayName = achievement.display_name.orEmpty(),
                    displayDescription = achievement.display_description.orEmpty(),
                    icon = CommunityAppImageUrl((displayData.appid ?: 0) to achievement.icon.orEmpty()),
                    unlockedPercent = (achievement.unlocked_pct ?: 0f).toDouble(),
                    hidden = achievement.hidden ?: false
                )
            }
        }

        // region Mapping content IDs to proper objects

        val totalAppIds = mutableListOf<AppId>()
        val totalUserIds = mutableListOf<SteamId>()

        val totalClanSteamIds = mutableListOf<SteamId>()
        val totalClanAnnouncementIds = mutableListOf<Long>()

        newsProto.news.asSequence().filter { event ->
            (event.gameid != null && event.gameid != 0L)
                    || (event.steamid_actor != null && event.steamid_actor != 0L)
                    || (event.appids.isNotEmpty())
                    || (event.clan_announcementid != null && event.clan_announcementid != 0L)
        }.forEach { event ->
            if (event.clan_announcementid != null) {
                totalClanSteamIds.add(event.steamid_actor.toSteamId())
                totalClanAnnouncementIds.add(event.clan_announcementid)
            } else {
                event.steamid_actor?.let { totalUserIds.add(SteamId(it.toULong())) }
            }

            event.gameid?.let(::AppId)?.let(totalAppIds::add)
            event.appids.map(::AppId).let(totalAppIds::addAll)
        }

        // endregion

        val totalSteamIds = (totalUserIds + totalClanSteamIds).distinctBy(SteamId::id)

        val summariesMap = steamClient.store.getAppSummaries(totalAppIds)
        val userMap = steamClient.persona.personas(totalSteamIds).first().associateBy { it.id }
        // val announcementMap = getEventDetails(eventIds = totalClanAnnouncementIds, clanIds = totalClanSteamIds).associateBy { it.gid }

        return newsProto.news.mapNotNull { event ->
            KSteamLogging.logVerbose("News:GetUserNews", event.toString())

            val eventType = EUserNewsType.byApiEnum(event.eventtype ?: 0) ?: return@mapNotNull null
            val actorSteamId = SteamId(event.steamid_actor?.toULong() ?: return@mapNotNull null)
            val eventDate = event.eventtime ?: return@mapNotNull null
            val actorPersona = userMap[actorSteamId] ?: Persona.Unknown

            when (eventType) {
                EUserNewsType.AchievementUnlocked -> {
                    val gameId = AppId(event.gameid?.toInt() ?: return@mapNotNull null)

                    ActivityFeedEntry.NewAchievements(
                        date = eventDate,
                        steamId = actorSteamId,
                        app = summariesMap[gameId] ?: return@mapNotNull null,
                        persona = actorPersona,
                        achievements = event.achievement_names.mapNotNull { acName ->
                            achievementMap[gameId]?.get(acName)
                        }
                    )
                }

                EUserNewsType.PlayedGameFirstTime -> {
                    val gameId = AppId(event.gameid?.toInt() ?: return@mapNotNull null)

                    ActivityFeedEntry.PlayedForFirstTime(
                        date = eventDate,
                        steamId = actorSteamId,
                        persona = actorPersona,
                        app = summariesMap[gameId] ?: return@mapNotNull null
                    )
                }

                EUserNewsType.ReceivedNewGame -> {
                    ActivityFeedEntry.ReceivedNewGame(
                        date = eventDate,
                        steamId = actorSteamId,
                        persona = actorPersona,
                        apps = event.appids.map(::AppId).mapNotNull { summariesMap[it] }
                    )
                }

                /*EUserNewsType.PostedAnnouncement -> {
                    ActivityFeedEntry.PostedAnnouncement(
                        date = eventDate,
                        steamId = actorSteamId,
                        persona = actorPersona,
                        announcement = event.clan_announcementid?.toString().let(announcementMap::get) ?: return@mapNotNull null,
                    )
                }*/

                else -> {
                    KSteamLogging.logDebug("News:GetUserNews", "Unknown event received, enum type: $eventType - dumping proto data below")
                    KSteamLogging.logDebug("News:GetUserNews", event.toString())

                    ActivityFeedEntry.UnknownEvent(
                        date = eventDate,
                        steamId = actorSteamId,
                        type = eventType,
                        persona = actorPersona,
                        proto = event
                    )
                }
            }
        }
    }

    /**
     * Returns event metadata by their IDs and owner clan SteamIDs.
     */
    suspend fun getEventDetails(
        eventIds: List<Long>,
        clanIds: List<SteamId>
    ): List<NewsEntry> {
        return steamClient.webApi.ajaxGetTyped<NewsCalendarResponse>(baseUrl = EnvironmentConstants.STORE_API_BASE, path = listOf("events", "ajaxgeteventdetails"), parameters = mapOf(
            "uniqueid_list" to eventIds.distinct().joinToString(separator = ","),
            "clanid_list" to clanIds.distinct().joinToString(separator = ",") { it.accountId.toString() }
        )).events
    }

    /**
     * A container for Steam user activity.
     */
    data class UserNews (
        /**
         * Entries for UI clients to show
         */
        val entries: List<ActivityFeedEntry>,
        /**
         * A paging link.
         */
        val nextFrom: Int
    )

    object UserNewsFilterScenario {
        /**
         * Mimics the filter in Steam Desktop's event feed when an app is selected.
         *
         * Shows: unlocked achievements, published screenshots + videos, user status ("Post about this game"), new user + curator reviews, wishlist additions, first-time play
         */
        val AppOverviewList = EUserNewsType.AchievementUnlocked + EUserNewsType.FilePublished_Screenshot + EUserNewsType.FilePublished_Video + EUserNewsType.UserStatus + EUserNewsType.RecommendedGame + EUserNewsType.CuratorRecommendedGame + EUserNewsType.AddedGameToWishlist + EUserNewsType.PlayedGameFirstTime // + EUserNewsType.PostedAnnouncement

        /**
         * Mimics what is shown in "Friend Activity" webpage
         *
         * Shows: unlocked achievements, published screenshots + videos, user status ("Post about this game"), new user + curator reviews, wishlist additions, first-time play, game events, added/removed friends
         */
        val FriendActivity = AppOverviewList + EUserNewsType.FriendAdded + EUserNewsType.FriendRemoved
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit
}