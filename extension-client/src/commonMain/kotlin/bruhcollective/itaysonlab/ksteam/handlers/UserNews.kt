package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.cdn.SteamCdn
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.enums.plus
import bruhcollective.itaysonlab.ksteam.models.news.usernews.ActivityFeedEntry
import bruhcollective.itaysonlab.ksteam.models.persona.SummaryPersona
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFile
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import steam.webui.usernews.CUserNews_GetUserNews_Request
import steam.webui.usernews.CUserNews_GetUserNews_Response
import kotlin.time.measureTimedValue

/**
 * Access Steam friend activity using this handler.
 */
class UserNews internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
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
    ): List<ActivityFeedEntry> = measure("getUserNews") {
        val newsProto = measure("getUserNews:getUserNews") {
            steamClient.unifiedMessages.execute(
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
        }

        // region Mapping achievements

        val achievementMap = mutableMapOf<String, ActivityFeedEntry.NewAchievements.Achievement>()

        measure("getUserNews:mapAchievements") {
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
        }

        // endregion

        // region Mapping content IDs to proper objects

        val totalAppIds = mutableListOf<Int>()
        val totalUserIds = mutableListOf<SteamId>()

        val totalClanSteamIds = mutableListOf<SteamId>()
        val totalClanAnnouncementIds = mutableListOf<Long>()

        val publishedFiles = mutableMapOf<Int, MutableList<Long>>()

        measure("getUserNews:mapIds") {
            newsProto.news.asSequence().filter { event ->
                (event.gameid != null && event.gameid != 0L)
                        || (event.steamid_actor != null && event.steamid_actor != 0L)
                        || (event.appids.isNotEmpty())
                        || (event.packageid != null && event.packageid != 0)
                        || (event.clan_announcementid != null && event.clan_announcementid != 0L)
                        || (event.publishedfileid != null && event.publishedfileid != 0L)
            }.forEach { event ->
                if (event.clan_announcementid != null && event.steamid_actor != 0L) {
                    totalClanSteamIds.add(event.steamid_actor.toSteamId())
                    totalClanAnnouncementIds.add(event.clan_announcementid)
                } else if (event.steamid_actor != 0L) {
                    event.steamid_actor?.let { totalUserIds.add(it.toSteamId()) }
                }

                if (event.gameid != 0L) {
                    totalAppIds.add(event.gameid?.toInt() ?: return@forEach)
                }

                if (event.appids.isNotEmpty()) {
                    totalAppIds.addAll(event.appids)
                }

                if (event.publishedfileid != null && event.publishedfileid != 0L) {
                    publishedFiles.getOrPut(event.gameid.toInt()) {
                        mutableListOf()
                    }.add(event.publishedfileid)
                }
            }
        }

        // endregion

        val totalSteamIds = (totalUserIds + totalClanSteamIds).distinctBy(SteamId::id)

        val summariesMap = measure("getUserNews:getAppSummaries") {
            steamClient.store.getAppSummaries(totalAppIds)
        }

        val userMap = measure("getUserNews:getProfileSummaries") {
            steamClient.profile.getProfileSummaries(totalSteamIds).associateBy { it.id }
        }

        val publishedFileMap = measure("getUserNews:getPublishedFiles") {
            publishedFiles.entries.flatMap { entry ->
                steamClient.publishedFiles.getDetails(entry.key, entry.value)
            }.associateBy(PublishedFile::id)
        }

        // val announcementMap = getEventDetails(eventIds = totalClanAnnouncementIds, clanIds = totalClanSteamIds).associateBy { it.gid }

        val entries = mutableListOf<ActivityFeedEntry>()

        val totalEventSize = newsProto.news.size
        val totalEventSizeLastIndex = newsProto.news.lastIndex

        val stringDuplicateStack = DuplicateStack<String>()
        val intDuplicateStack = DuplicateStack<Int>()
        val longDuplicateStack = DuplicateStack<Long>()

        for (i in 0..<totalEventSize) {
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

                    val appSummary = summariesMap[event.gameid?.toInt() ?: continue] ?: continue
                    val achievements = stringDuplicateStack.use { saved -> event.achievement_names + saved }.mapNotNull(achievementMap::get)

                    ActivityFeedEntry.NewAchievements(
                        date = eventDate,
                        app = appSummary,
                        persona = actorPersona,
                        achievements = achievements
                    )
                }

                EUserNewsType.PlayedGameFirstTime -> {
                    val appSummary = summariesMap[event.gameid?.toInt() ?: continue] ?: continue

                    ActivityFeedEntry.PlayedForFirstTime(
                        date = eventDate,
                        persona = actorPersona,
                        app = appSummary
                    )
                }

                EUserNewsType.ReceivedNewGame -> {
                    if (ActivityFeedEntry.ReceivedNewGame.canMergeWith(event, eventNext)) {
                        intDuplicateStack += event.appids
                        continue
                    }

                    val apps = intDuplicateStack.use { saved -> event.appids + saved }
                        .mapNotNull { summariesMap[it] }

                    ActivityFeedEntry.ReceivedNewGame(
                        date = eventDate,
                        persona = actorPersona,
                        apps = apps,
                        packages = emptyList()
                    )
                }

                EUserNewsType.AddedGameToWishlist -> {
                    if (ActivityFeedEntry.AddedToWishlist.canMergeWith(event, eventNext)) {
                        intDuplicateStack += event.gameid?.toInt() ?: continue
                        continue
                    }

                    val apps = intDuplicateStack.use { saved ->
                        listOf(
                            event.gameid?.toInt() ?: 0
                        ) + saved
                    }.mapNotNull { summariesMap[it] }

                    ActivityFeedEntry.AddedToWishlist(
                        date = eventDate,
                        persona = actorPersona,
                        apps = apps
                    )
                }

                EUserNewsType.FilePublished_Screenshot -> {
                    if (ActivityFeedEntry.ScreenshotPosted.canMergeWith(event, eventNext)) {
                        longDuplicateStack += event.publishedfileid ?: continue
                        continue
                    }

                    val screenshots = longDuplicateStack.use { saved -> listOf(event.publishedfileid) + saved }.mapNotNull { publishedFileMap[it] }.filterIsInstance<PublishedFile.Screenshot>()
                    val appSummary = summariesMap[event.gameid?.toInt() ?: continue] ?: continue

                    if (screenshots.size == 1) {
                        ActivityFeedEntry.ScreenshotPosted(
                            date = eventDate,
                            persona = actorPersona,
                            app = appSummary,
                            screenshot = screenshots.first()
                        )
                    } else {
                        ActivityFeedEntry.ScreenshotsPosted(
                            date = eventDate,
                            persona = actorPersona,
                            app = appSummary,
                            screenshots = screenshots
                        )
                    }
                }

                else -> {
                    KSteamLogging.logDebug("News:GetUserNews") { "Unknown event received, enum type: $eventType - dumping proto data below" }
                    KSteamLogging.logDebug("News:GetUserNews") { event.toString() }

                    ActivityFeedEntry.UnknownEvent(
                        date = eventDate,
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
        // EUserNewsType.PostedAnnouncement but it is unresolvable in Steam3 API
        val AppOverviewList = EUserNewsType.AchievementUnlocked + EUserNewsType.FilePublished_Screenshot + EUserNewsType.FilePublished_Video + EUserNewsType.UserStatus + EUserNewsType.RecommendedGame + EUserNewsType.CuratorRecommendedGame + EUserNewsType.AddedGameToWishlist + EUserNewsType.PlayedGameFirstTime

        /**
         * Mimics what is shown in "Friend Activity" webpage
         *
         * Shows: unlocked achievements, published screenshots + videos, user status ("Post about this game"), new user + curator reviews, wishlist additions, first-time play, added/removed friends
         */
        val FriendActivity = AppOverviewList + EUserNewsType.FriendAdded + EUserNewsType.FriendRemoved
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit

    private inline fun <T> measure(label: String, func: () -> T): T {
        return measureTimedValue(func).also {
            KSteamLogging.logDebug("UserNews") { "[measure] $label done in ${it.duration.inWholeMilliseconds} ms" }
        }.value
    }

    private class DuplicateStack <T> {
        private val stack = mutableListOf<T>()

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