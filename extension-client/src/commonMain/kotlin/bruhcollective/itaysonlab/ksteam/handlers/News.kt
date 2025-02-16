package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.handlers.Logger.Verbosity
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.news.*
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubResponse
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.time.measureTimedValue

/**
 * Access Steam news using this handler.
 */
class News internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    private companion object {
        private const val LOG_TAG = "CoreExt:News"
    }

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
    }

    private val currentSeconds get() = Clock.System.now().epochSeconds
    private var clanCache = mutableMapOf<SteamId, ClanSummary>()

    /**
     * Returns upcoming events.
     */
    suspend fun getUpcomingEvents(
        maxCount: Int = 250,
        eventTypes: List<NewsEventType> = NewsEventType.Collections.Everything,
        appTypes: List<NewsAppType> = NewsAppType.Default,
    ) = getEventsInCalendarRange(
        range = currentSeconds..0,
        maxCount = maxCount,
        eventTypes = eventTypes,
        appTypes = appTypes,
        ascending = true
    )

    /**
     * Get events in the given calendar range.
     *
     * @param range between two unix timestamps
     * @param collectionId the collection ID for news. For example, it could be "steam" for the Global News -> Steam Official section of the official news app. For ease of use, Steam's collection IDs are provided in [Collections] object.
     * @param maxCount maximum count of news to return
     * @param ascending sort from the earliest event
     * @param eventTypes show only chosen event types
     * @param appTypes show only chosen app types
     * @param filterByAppIds get data only for specific application IDs
     * @param filterByClanIds get data only for specific [SteamId] with a "clan" state
     *
     * @return a well-formed and parsed page ready to be shown in "News" screen
     */
    suspend fun getEventsInCalendarRange(
        range: LongRange = 0..currentSeconds,
        collectionId: String? = null,
        maxCount: Int = 250,
        ascending: Boolean = false,
        eventTypes: List<NewsEventType> = NewsEventType.Collections.Everything,
        appTypes: List<NewsAppType> = NewsAppType.Default,
        filterByAppIds: List<Int> = emptyList(),
        filterByClanIds: List<SteamId> = emptyList()
    ): List<NewsEvent> = measure("getEventsInCalendarRange") {
        val calendar = measure("getEventsInCalendarRange:getCalendarRange") {
            steamClient.webApi.store.method("events/ajaxgetusereventcalendarrange/") {
                "minTime" with range.first
                "maxTime" with range.last

                "ascending" with ascending
                "maxResults" with maxCount

                "populateEvents" with "30" // the number of events that are returned in the batch, server max is 30

                "appTypes" with appTypes.joinToString(separator = ",", transform = NewsAppType::apiName)
                "eventTypes" with eventTypes.joinToString(separator = ",") { it.ordinal.toString() }

                "collectionID" with collectionId // featured, steam, press
            }.body<NewsCalendarResponse>()
        }

        // Get documents aka events references
        // 30 is the chunk limit in SteamJS

        steamClient.logger.logDebug(LOG_TAG) { "[getEventsInCalendarRange] requesting event entries, total: ${calendar.documents.size}" }

        val entries = calendar.events.toMutableList()

        measure("getEventsInCalendarRange:getAdditionalEventDetails") {
            if (calendar.documents.size != calendar.events.size) {
                val alreadyReturned = calendar.events.map(NewsEntry::gid)

                calendar.documents
                    .filterNot { alreadyReturned.contains(it.uniqueId) }
                    .chunked(30)
                    .forEach { docChunk ->
                        entries += getEventDetails(docChunk)
                    }
            }
        }

        // Get app summaries

        steamClient.logger.logDebug(LOG_TAG) { "[getEventsInCalendarRange] requesting apps, total: ${calendar.apps.size}" }

        val appsMap = calendar.apps.associateBy { AppId(it.appId) }

        val apps = measure("getEventsInCalendarRange:getAppSummaries") {
            steamClient.store.getNetworkSteamApplications(appsMap.keys.toList()).associateBy(SteamApplication::id)
        }

        // Get clans summaries

        steamClient.logger.logDebug(LOG_TAG) { "[getEventsInCalendarRange] requesting clans, total: ${calendar.clans.size}" }

        val clans = measure("getEventsInCalendarRange:requestClanPersonas") {
            entries.asSequence().filter { entry ->
                entry.appid == 0 && entry.clanSteamid.isNotEmpty()
            }.map {
                it.clanSteamid.toULongOrNull().toSteamId()
            }.toList().map {
                resolveClanInfo(it)
            }.associateBy { it.steamId }
        }

        // Now we have all required data, we can parse them to NewsEvent's

        return entries.map { entry ->
            val jsonDescription = json.decodeFromString<NewsJsonData>(entry.jsondata.let {
                if (it == "[]") {
                    "{}"  // why do they provide broken data
                } else {
                    it
                }
            })

            val clanSteamid = SteamId(entry.clanSteamid.toULong())

            val headerImage = if (entry.videoPreviewType == "youtube") {
                "https://img.youtube.com/vi/${entry.videoPreviewId}/maxresdefault.jpg"
            } else {
                val engPhoto = jsonDescription.titleImages.firstOrNull().orEmpty()

                if (engPhoto.isNotEmpty()) {
                    "https://clan.akamai.steamstatic.com/images/${clanSteamid.accountId}/${engPhoto}"
                } else {
                    ""
                }
            }

            NewsEvent(
                id = entry.gid,
                type = NewsEventType.entries.getOrElse(entry.eventType) { NewsEventType.Unknown },
                clanSteamId = SteamId(entry.clanSteamid.toULong()),
                creatorSteamId = SteamId(entry.creatorSteamid.toULong()),
                updaterSteamId = SteamId(entry.lastUpdateSteamid.toULong()),
                clanSummary = clans[entry.clanSteamid],
                title = entry.eventName,
                subtitle = jsonDescription.subtitles.firstOrNull().orEmpty(),
                description = jsonDescription.summaries.firstOrNull().orEmpty(),
                header = headerImage,
                capsule = jsonDescription.capsuleImages.firstOrNull().orEmpty(),
                likeCount = entry.votesUp,
                dislikeCount = entry.votesDown,
                commentCount = entry.commentCount,
                forumTopicId = entry.forumTopicId,
                publishedAt = entry.announcementBody.posttime,
                lastUpdatedAt = entry.announcementBody.updatetime,
                relatedApp = apps[AppId(entry.appid)],
                content = entry.announcementBody.body,
                eventStartDate = entry.rtime32StartTime,
                eventEndDate = entry.rtime32EndTime,
                recommended = ((appsMap[AppId(entry.appid)]?.source ?: 0) and NewsCalendarResponseApp.SourceFlags.Recommended.bitmask) != 0
            )
        }
    }

    /**
     * Get community hub - UGC content related to a specific game, such as photos/guides/videos/reviews.
     *
     * @param appId
     */
    suspend fun getCommunityHub(
        appId: Int
    ): List<CommunityHubPost> {
        return steamClient.webApi.community.method("library/appcommunityfeed/${appId}") {
            "p" with 1
            "nMaxInappropriateScore" with 1
            "filterLanguage" with steamClient.language.vdfName
            "languageTag" with steamClient.language.vdfName
            "rgSections[]" with listOf("2", "3", "4", "9")
        }.body<CommunityHubResponse>().hub
    }

    /**
     * Returns event metadata by their IDs and owner clan SteamIDs.
     */
    suspend fun getEventDetails(
        eventIds: List<Long>,
        clanIds: List<SteamId>
    ): List<NewsEntry> {
        return steamClient.webApi.store.method("events/ajaxgeteventdetails") {
            "uniqueid_list" with eventIds.distinct().joinToString(separator = ",")
            "clanid_list" with clanIds.distinct().joinToString(separator = ",") { it.accountId.toString() }
        }.body<NewsCalendarResponse>().events
    }

    private suspend fun getEventDetails(
        documents: List<NewsDocument>
    ): List<NewsEntry> {
        return steamClient.webApi.store.method("events/ajaxgeteventdetails") {
            "uniqueid_list" with documents.joinToString(separator = ",") { doc -> doc.uniqueId }
            "clanid_list" with documents.joinToString(separator = ",") { clan -> clan.clanId.toString() }
        }.body<NewsCalendarResponse>().events
    }

    private suspend fun resolveClanInfo(
        id: SteamId
    ): ClanSummary {
        return clanCache.getOrPut(id) {
            steamClient.webApi.community.method("gid/$id/ajaxgetvanityandclanid/").body<ClanSummary>()
        }
    }

    //

    private inline fun <T> measure(label: String, func: () -> T): T {
        return if (steamClient.logger.verbosity.atLeast(Verbosity.Debug)) {
            measureTimedValue(func).also {
                steamClient.logger.logDebug(LOG_TAG) { "[measure] $label done in ${it.duration.inWholeMilliseconds} ms" }
            }.value
        } else {
            func()
        }
    }

    object Collections {
        const val Featured = "featured"
        const val Steam = "steam"
        const val Press = "press"
    }
}