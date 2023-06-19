package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.news.AppType
import bruhcollective.itaysonlab.ksteam.models.news.ClanSummary
import bruhcollective.itaysonlab.ksteam.models.news.NewsCalendarResponse
import bruhcollective.itaysonlab.ksteam.models.news.NewsCalendarResponseApp
import bruhcollective.itaysonlab.ksteam.models.news.NewsDocument
import bruhcollective.itaysonlab.ksteam.models.news.NewsEntry
import bruhcollective.itaysonlab.ksteam.models.news.NewsEvent
import bruhcollective.itaysonlab.ksteam.models.news.NewsEventType
import bruhcollective.itaysonlab.ksteam.models.news.NewsJsonData
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubResponse
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Access Steam news using this handler.
 */
class News internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
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
        eventTypes: Array<NewsEventType> = NewsEventType.Collections.Everything,
        appTypes: Array<AppType> = AppType.Default,
    ) = getEventsInCalendarRange(
        range = currentSeconds..0,
        maxCount = maxCount,
        eventTypes = eventTypes,
        appTypes = appTypes
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
        eventTypes: Array<NewsEventType> = NewsEventType.Collections.Everything,
        appTypes: Array<AppType> = AppType.Default,
        filterByAppIds: List<Int> = emptyList(),
        filterByClanIds: List<SteamId> = emptyList(),
    ): List<NewsEvent> {
        val calendar = steamClient.webApi.store.method("events/ajaxgetusereventcalendarrange/") {
            "minTime" with range.first
            "maxTime" with range.last

            "ascending" with ascending
            "maxResults" with maxCount

            "populateEvents" with "15" // research

            "appTypes" with appTypes.joinToString(separator = ",", transform = AppType::apiName)
            "eventTypes" with eventTypes.joinToString(separator = ",") { it.ordinal.toString() }

            "collectionID" with collectionId // featured, steam, press
        }.body<NewsCalendarResponse>()

        // Get documents aka events references
        // 30 is the chunk limit in SteamJS

        KSteamLogging.logDebug(LOG_TAG) { "[getEventsInCalendarRange] requesting event entries, total: ${calendar.documents.size}" }

        val alreadyReturned = calendar.events.map(NewsEntry::gid)
        val entries = calendar.events.toMutableList()

        calendar.documents.filterNot { alreadyReturned.contains(it.uniqueId) }.chunked(30).forEach { docChunk ->
            entries += getEventDetails(docChunk)
        }

        // Get app summaries

        KSteamLogging.logDebug(LOG_TAG) { "[getEventsInCalendarRange] requesting apps, total: ${calendar.apps.size}" }
        val apps = steamClient.store.getAppSummaries(calendar.apps.map(NewsCalendarResponseApp::appId))

        // Get clans summaries

        KSteamLogging.logDebug(LOG_TAG) { "[getEventsInCalendarRange] requesting clans, total: ${calendar.clans.size}" }

        val clans = entries.asSequence().filter { entry ->
            entry.appid == 0 && entry.clanSteamid.isNotEmpty()
        }.associate { entry ->
            entry.clanSteamid to resolveClanInfo(entry.clanSteamid.toULongOrNull().toSteamId())
        }

        KSteamLogging.logDebug(LOG_TAG) { "[getEventsInCalendarRange] clans requested, total: ${clans.size}" }

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
                type = NewsEventType.values().getOrElse(entry.eventType) { NewsEventType.Unknown },
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
                relatedApp = apps[entry.appid],
                content = entry.announcementBody.body,
                eventStartDate = entry.rtime32StartTime,
                eventEndDate = entry.rtime32EndTime,
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

    override suspend fun onEvent(packet: SteamPacket) = Unit

    object Collections {
        const val Featured = "featured"
        const val Steam = "steam"
        const val Press = "press"
    }
}