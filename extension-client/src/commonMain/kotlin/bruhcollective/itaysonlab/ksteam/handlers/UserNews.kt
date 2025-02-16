package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.enums.plus
import bruhcollective.itaysonlab.ksteam.models.news.usernews.ActivityFeedEntry
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFile
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import steam.webui.player.CPlayer_GetPostedStatus_Request
import steam.webui.usernews.CUserNews_GetUserNews_Request
import kotlin.time.measureTimedValue

/**
 * Access Steam friend activity using this handler.
 */
class UserNews internal constructor(
    private val steamClient: ExtendedSteamClient
) {
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
        count: Int? = 100,
        startTime: Int = 0,
        endTime: Int = 0,
    ): List<ActivityFeedEntry> = measure("getUserNews") {
        val newsProto = measure("getUserNews:getUserNews") {
            steamClient.grpc.userNews.GetUserNews().executeSteam(
                data = CUserNews_GetUserNews_Request(
                    filterappid = appId,
                    filterflags = showEvents,
                    count = count,
                    starttime = startTime,
                    endtime = endTime,
                    language = steamClient.language.vdfName
                )
            )
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
                        icon = EnvironmentConstants.formatCommunityImageUrl(achAppId, achievement.icon.orEmpty()),
                        unlockedPercent = (achievement.unlocked_pct ?: 0f).toDouble(),
                        hidden = achievement.hidden == true
                    )
                }
            }
        }

        // endregion

        // region Mapping content IDs to proper objects

        val totalAppIds = mutableListOf<AppId>()
        val totalUserIds = mutableListOf<SteamId>()

        val totalClanSteamIds = mutableListOf<SteamId>()
        val totalClanAnnouncementIds = mutableListOf<Long>()

        val publishedFiles = mutableMapOf<Int, MutableList<Long>>()
        val totalPostedStatuses = mutableListOf<Pair<Long, Long>>()

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
                    totalClanAnnouncementIds.add(event.clan_announcementid!!)
                } else if (event.steamid_actor != 0L) {
                    event.steamid_actor?.let { totalUserIds.add(it.toSteamId()) }
                }

                if (event.gameid != 0L) {
                    totalAppIds.add(AppId(event.gameid?.toInt() ?: return@forEach))
                }

                if (event.appids.isNotEmpty()) {
                    totalAppIds.addAll(event.appids.map(::AppId))
                }

                if (event.publishedfileid != null && event.publishedfileid != 0L) {
                    publishedFiles.getOrPut(event.gameid!!.toInt()) {
                        mutableListOf()
                    }.add(event.publishedfileid!!)
                }

                if (event.eventtype == EUserNewsType.UserStatus.apiEnum && event.gameid != null && event.steamid_target != 0L) {
                    totalPostedStatuses += ((event.steamid_target ?: 0) to (event.steamid_actor ?: 0))
                }
            }
        }

        // endregion

        val totalSteamIds = (totalUserIds + totalClanSteamIds).distinctBy(SteamId::id)

        coroutineScope {
            val summariesMapRequest = async {
                measure("getUserNews:getAppSummaries") {
                    steamClient.store.querySteamApplications(totalAppIds).associateBy(SteamApplication::id)
                }
            }

            val userMapRequest = async {
                measure("getUserNews:getProfileSummaries") {
                    steamClient.profile.queryPersonas(totalUserIds.distinctBy(SteamId::id)).associateBy { it.id }
                }
            }

            val publishedFileMapRequest = async {
                measure("getUserNews:getPublishedFiles") {
                    publishedFiles.entries.flatMap { entry ->
                        steamClient.publishedFiles.getDetails(entry.key, entry.value)
                    }.associateBy(PublishedFile::id)
                }
            }

            val userStatusMapRequests = async {
                measure("getUserNews:getPostedStatuses") {
                    totalPostedStatuses.map { (postId, steamId) ->
                        steamClient.grpc.player.GetPostedStatus().executeSteam(
                            data = CPlayer_GetPostedStatus_Request(
                                steamid = steamId,
                                postid = postId,
                            )
                        )
                    }.associateBy {
                       it.postid ?: 0
                    }
                }
            }

            val summariesMap = summariesMapRequest.await()
            val userMap = userMapRequest.await()
            val publishedFileMap = publishedFileMapRequest.await()
            val userStatusMap = userStatusMapRequests.await()

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
                val actorPersona = userMap[actorSteamId] ?: Persona.Unknown

                when (eventType) {
                    EUserNewsType.AchievementUnlocked -> {
                        if (ActivityFeedEntry.NewAchievements.canMergeWith(event, eventNext)) {
                            stringDuplicateStack += event.achievement_names
                            continue
                        }

                        val appSummary = summariesMap[AppId(event.gameid?.toInt() ?: continue)] ?: continue
                        val achievements = stringDuplicateStack.use { saved -> event.achievement_names + saved }.mapNotNull(achievementMap::get)

                        ActivityFeedEntry.NewAchievements(
                            date = eventDate,
                            app = appSummary,
                            persona = actorPersona,
                            achievements = achievements
                        )
                    }

                    EUserNewsType.UserStatus -> {
                        val appSummary = summariesMap[AppId(event.gameid?.toInt() ?: continue)] ?: continue
                        val postStatus = userStatusMap[event.steamid_target ?: continue] ?: continue

                        ActivityFeedEntry.PostedStatus(
                            date = eventDate,
                            app = appSummary,
                            persona = actorPersona,
                            postId = postStatus.postid ?: 0,
                            text = postStatus.status_text.orEmpty(),
                        )
                    }

                    EUserNewsType.PlayedGameFirstTime -> {
                        val appSummary = summariesMap[AppId(event.gameid?.toInt() ?: continue)] ?: continue

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
                            .mapNotNull { summariesMap[AppId(it)] }

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
                        }.mapNotNull { summariesMap[AppId(it)] }

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
                        val appSummary = summariesMap[AppId(event.gameid?.toInt() ?: continue)] ?: continue

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

                    EUserNewsType.FriendAdded -> {
                        if (ActivityFeedEntry.FriendAdded.canMergeWith(event, eventNext)) {
                            longDuplicateStack += event.steamid_target ?: continue
                            continue
                        }

                        val personas = longDuplicateStack.use { saved -> listOf(event.steamid_target ?: 0L) + saved }
                            .mapNotNull { userMap[SteamId(it.toULong())] }

                        ActivityFeedEntry.FriendAdded(
                            date = eventDate,
                            persona = actorPersona,
                            addedPersonas = personas
                        )
                    }

                    else -> {
                        steamClient.logger.logDebug("News:GetUserNews") { "Unknown event received, enum type: $eventType - dumping proto data below" }
                        steamClient.logger.logDebug("News:GetUserNews") { event.toString() }

                        ActivityFeedEntry.UnknownEvent(
                            date = eventDate,
                            type = eventType,
                            persona = actorPersona,
                            proto = event
                        )
                    }
                }.let(entries::add)
            }

            return@coroutineScope entries
        }
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
        // EUserNewsType.CuratorRecommendedGame, EUserNewsType.PostedAnnouncement but it is unresolvable in Steam3 API
        val AppOverviewList = EUserNewsType.AchievementUnlocked +
                EUserNewsType.FilePublished_Screenshot +
                EUserNewsType.FilePublished_Video +
                EUserNewsType.UserStatus +
                EUserNewsType.RecommendedGame +
                EUserNewsType.AddedGameToWishlist +
                EUserNewsType.PlayedGameFirstTime

        /**
         * Mimics what is shown in "Friend Activity" webpage
         *
         * According to Steam:
         * - Friends
         * - - adds a friend [FriendAdded]
         * - - earns achievements [AchievementUnlocked]
         * - - buys/preorders a game [ReceivedNewGame]
         * - - joins group [JoinedGroup]
         * - - creates group [GroupCreated]
         * - - adds to wishlist [AddedGameToWishlist]
         * - - publishes a review [RecommendedGame]
         * - - publishes a screenshot [FilePublished_Screenshot]
         * - - publishes a video [FilePublished_Video]
         * - - favorites a workshop item (guides)
         * - - [PlayedGameFirstTime], [UserStatus] (not on web settings)
         *
         * - Groups (skipped)
         * - Workshop (skipped)
         *
         * - Curators
         * - - recommends a game [CuratorRecommendedGame] (not resolvable on Steam API)
         *
         * - Workshop / Users (skipped)
         *
         * - When I
         * - - was tagged on a screenshot [FilePublished_Screenshot_Tagged]
         *
         * Shows: unlocked achievements, published screenshots + videos, user status ("Post about this game"), new user + curator reviews, wishlist additions, first-time play, added/removed friends
         */
        val FriendActivity = AppOverviewList + EUserNewsType.FriendAdded
    }

    private inline fun <T> measure(label: String, func: () -> T): T {
        return measureTimedValue(func).also {
            steamClient.logger.logDebug("UserNews") { "[measure] $label done in ${it.duration.inWholeMilliseconds} ms" }
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