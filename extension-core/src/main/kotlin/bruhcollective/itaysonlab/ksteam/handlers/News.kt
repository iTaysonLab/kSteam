package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.cdn.CommunityAppImageUrl
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.enums.plus
import NewsCalendarResponse
import bruhcollective.itaysonlab.ksteam.models.news.AppType
import bruhcollective.itaysonlab.ksteam.models.news.EventType
import bruhcollective.itaysonlab.ksteam.models.news.NewsCalendarResponse
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubResponse
import bruhcollective.itaysonlab.ksteam.models.news.usernews.ActivityFeedEntry
import community.CommunityHubResponse
import usernews.ActivityFeedEntry
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
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
     * Get community hub for a specific [AppId].
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
            extraParameters = {
                appendAll("rgSections[]", listOf("2", "3", "4", "9"))
            }
        ).hub
    }

    /**
     * Get activity events for a specific [AppId].
     */
    suspend fun getUserNews(
        appId: AppId,
        count: Int = 100
    ): List<ActivityFeedEntry> {
        val newsProto = steamClient.unifiedMessages.execute(
            methodName = "UserNews.GetUserNews",
            requestAdapter = CUserNews_GetUserNews_Request.ADAPTER,
            responseAdapter = CUserNews_GetUserNews_Response.ADAPTER,
            requestData = CUserNews_GetUserNews_Request(
                filterappid = appId.id,
                filterflags = EUserNewsType.AchievementUnlocked + EUserNewsType.FilePublished_Screenshot + EUserNewsType.FilePublished_Video + EUserNewsType.UserStatus + EUserNewsType.RecommendedGame + EUserNewsType.AddedGameToWishlist + EUserNewsType.PlayedGameFirstTime + EUserNewsType.PostedAnnouncement,
                count = count,
                starttime = 0,
                endtime = 0,
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

        val summariesMap = (newsProto.news.mapNotNull { event ->
            event.gameid?.toInt()
        }.filter { it != 0 } + newsProto.news.map { event ->
            event.appids
        }.flatten()).map(::AppId).let {
            steamClient.store.getAppSummaries(it)
        }

        val userMap = newsProto.news.mapNotNull { event ->
            SteamId(event.steamid_actor?.toULong() ?: return@mapNotNull null)
        }.let {
            steamClient.persona.personas(it)
        }.first().associateBy { it.id }

        return newsProto.news.mapNotNull { event ->
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

                else -> {
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

    override suspend fun onEvent(packet: SteamPacket) = Unit
}