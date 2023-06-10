package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.cdn.SteamCdn
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
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
import bruhcollective.itaysonlab.ksteam.models.persona.SummaryPersona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
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
        filterByAppIds: List<Int> = emptyList(),
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
        appId: Int
    ): List<CommunityHubPost> {
        return steamClient.webApi.ajaxGetTyped<CommunityHubResponse>(
            path = listOf("library", "appcommunityfeed", appId.toString()),
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
        appId: Int = 0,
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
                filterappid = appId,
                filterflags = showEvents,
                count = count,
                starttime = startTime,
                endtime = endTime,
                language = steamClient.language.vdfName
            )
        ).data

        // region Mapping achievements

        val achievementMap = mutableMapOf<String, ActivityFeedEntry.NewAchievements.Achievement>()

        newsProto.achievement_display_data.forEach { displayData ->
            val achAppId = displayData.appid ?: return@forEach

            displayData.achievements.forEach { achievement ->
                achievementMap[achievement.name.orEmpty()] = ActivityFeedEntry.NewAchievements.Achievement(
                    internalName = achievement.name.orEmpty(),
                    displayName = achievement.display_name.orEmpty(),
                    displayDescription = achievement.display_description.orEmpty(),
                    icon = SteamCdn.formatCommunityImageUrl(achAppId, achievement.icon.orEmpty()),
                    unlockedPercent = (achievement.unlocked_pct ?: 0f).toDouble(),
                    hidden = achievement.hidden ?: false
                )
            }
        }

        // endregion

        // region Mapping content IDs to proper objects

        val totalAppIds = mutableListOf<Int>()
        val totalUserIds = mutableListOf<SteamId>()

        val totalClanSteamIds = mutableListOf<SteamId>()
        val totalClanAnnouncementIds = mutableListOf<Long>()

        newsProto.news.asSequence().filter { event ->
            (event.gameid != null && event.gameid != 0L)
                    || (event.steamid_actor != null && event.steamid_actor != 0L)
                    || (event.appids.isNotEmpty())
                    || (event.packageid != null && event.packageid != 0)
                    || (event.clan_announcementid != null && event.clan_announcementid != 0L)
        }.forEach { event ->
            if (event.clan_announcementid != null) {
                totalClanSteamIds.add(event.steamid_actor.toSteamId())
                totalClanAnnouncementIds.add(event.clan_announcementid)
            } else {
                event.steamid_actor?.let { totalUserIds.add(it.toSteamId()) }
            }

            event.gameid?.toInt()?.let(totalAppIds::add)
            event.appids.let(totalAppIds::addAll)
        }

        // endregion

        val totalSteamIds = (totalUserIds + totalClanSteamIds).distinctBy(SteamId::id)
        val summariesMap = steamClient.store.getAppSummaries(totalAppIds)
        val userMap = steamClient.profile.getProfileSummaries(totalSteamIds).associateBy { it.id }

        // val announcementMap = getEventDetails(eventIds = totalClanAnnouncementIds, clanIds = totalClanSteamIds).associateBy { it.gid }

        val entries = mutableListOf<ActivityFeedEntry>()

        val totalEventSize = newsProto.news.size
        val totalEventSizeLastIndex = newsProto.news.lastIndex

        val stringDuplicateStack = DuplicateStack<String>()
        val intDuplicateStack = DuplicateStack<Int>()

        for (i in 0 until totalEventSize) {
            val event = newsProto.news[i]
            val eventNext = newsProto.news.getOrNull(i + 1)

            val eventType = EUserNewsType.byApiEnum(event.eventtype ?: 0) ?: continue
            val actorSteamId = SteamId(event.steamid_actor?.toULong() ?: continue)
            val eventDate = event.eventtime ?: continue
            val actorPersona = userMap[actorSteamId] ?: SummaryPersona.Unknown

            when (eventType) {
                EUserNewsType.AchievementUnlocked -> {
                    if (ActivityFeedEntry.NewAchievements.canMergeWith(event, eventNext)) {
                        stringDuplicateStack += event.achievement_names
                        continue
                    }

                    val gameId = event.gameid?.toInt() ?: continue
                    val achievements = stringDuplicateStack.use { saved -> event.achievement_names + saved }.mapNotNull(achievementMap::get)

                    ActivityFeedEntry.NewAchievements(
                        date = eventDate,
                        steamId = actorSteamId,
                        app = summariesMap[gameId] ?: continue,
                        persona = actorPersona,
                        achievements = achievements
                    )
                }

                EUserNewsType.PlayedGameFirstTime -> {
                    // val gameId = AppId(event.gameid?.toInt() ?: continue)

                    ActivityFeedEntry.PlayedForFirstTime(
                        date = eventDate,
                        steamId = actorSteamId,
                        persona = actorPersona,
                        apps = emptyList() // summariesMap[gameId] ?: continue
                    )
                }

                EUserNewsType.ReceivedNewGame -> {
                    if (ActivityFeedEntry.ReceivedNewGame.canMergeWith(event, eventNext)) {
                        intDuplicateStack += event.appids
                        continue
                    }

                    val apps = intDuplicateStack.use { saved -> event.appids + saved }.mapNotNull { summariesMap[it] }

                    ActivityFeedEntry.ReceivedNewGame(
                        date = eventDate,
                        steamId = actorSteamId,
                        persona = actorPersona,
                        apps = apps,
                        packages = emptyList()
                    )
                }

                EUserNewsType.AddedGameToWishlist -> {
                    if (ActivityFeedEntry.ReceivedNewGame.canMergeWith(event, eventNext)) {
                        intDuplicateStack += event.appids
                        continue
                    }

                    val apps = intDuplicateStack.use { saved -> event.appids + saved }.mapNotNull { summariesMap[it] }

                    ActivityFeedEntry.ReceivedNewGame(
                        date = eventDate,
                        steamId = actorSteamId,
                        persona = actorPersona,
                        apps = apps,
                        packages = emptyList()
                    )
                }


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
            }.let(entries::add)
        }

        return entries
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

    private class DuplicateStack <T> {
        private val stack = mutableListOf<T>()

        fun push(item: T) {
            stack.add(item)
        }

        fun <Out> use(action: (List<T>) -> Out): Out {
            return try {
                action(stack)
            } finally {
                stack.clear()
            }
        }

        operator fun plusAssign(item: T) {
            stack.add(item)
        }

        operator fun plusAssign(list: List<T>) {
            stack.addAll(list)
        }
    }
}