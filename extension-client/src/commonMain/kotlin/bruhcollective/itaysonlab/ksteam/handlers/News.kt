package bruhcollective.itaysonlab.ksteam.handlers

import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableScatterMapOf
import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.handlers.Logger.Verbosity
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.news.*
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost
import bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubResponse
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.toDuration

/**
 * Access Steam news using this handler.
 */
@OptIn(ExperimentalTime::class)
class News internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    private companion object {
        private const val LOG_TAG = "CoreExt:News"

        const val SOURCE_LIBRARY = 1
        const val SOURCE_WISHLIST = 2
        const val SOURCE_FOLLOWING = 4
        const val SOURCE_RECOMMENDED = 8
        const val SOURCE_STEAM = 16
        const val SOURCE_REQUIRED = 32
        const val SOURCE_FEATURED = 64
        const val SOURCE_CURATOR = 128
        const val SOURCE_REPOSTED = 256
    }

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
    }

    private val currentSeconds get() = Clock.System.now().epochSeconds
    private var clanCache = mutableIntObjectMapOf<ClanSummary>()

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
        saleId: String? = null,
        hubType: String? = null,
        categoryOrLanguage: String? = null,
        tagName: String? = null,
        maxCount: Int = 250,
        ascending: Boolean = false,
        eventTypes: List<NewsEventType> = NewsEventType.Collections.Everything,
        appTypes: List<NewsAppType> = NewsAppType.Default,
        filterByAppIds: List<Int> = emptyList(),
        filterByClanIds: List<SteamId> = emptyList(),
        filterByTags: List<Int> = emptyList()
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

                if (filterByAppIds.isNotEmpty()) {
                    "appIdFilter" with filterByAppIds.sorted().joinToString(separator = ",")
                }

                if (filterByClanIds.isNotEmpty()) {
                    "clanIdFilter" with filterByClanIds.sortedBy(SteamId::id).joinToString(separator = ",")
                }

                if (filterByTags.isNotEmpty()) {
                    "tags" with filterByTags.sorted().joinToString(separator = ",")
                }

                "collectionID" with collectionId // featured, steam, press
                "saleID" with saleId
                "hubtype" with hubType
                "category_or_language" with categoryOrLanguage
                "tag_name" with tagName
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

        steamClient.logger.logDebug(LOG_TAG) { "[getEventsInCalendarRange] parsing entries: ${entries.size}" }

        return parseNewsEntries(
            entries = entries,
            appsSourceMap = calendar.apps.associateBy { AppId(it.appId) },
            clanSourceMap = calendar.clans.associateBy(NewsCalendarResponseClan::clanId),
        )
    }

    suspend fun getAdjacentPartnerEvents(
        clanAccountId: Int,
        appId: Int,
        countBefore: Int,
        countAfter: Int,
        gidEvent: String,
        gidAnnouncement: String,
        languages: List<ELanguage>
    ): List<NewsEvent> {
        val calendar = steamClient.webApi.store.method("events/ajaxgetadjacentpartnerevents/") {
            "clan_accountid" with clanAccountId
            "appid" with appId
            "count_before" with countBefore
            "count_after" with countAfter
            "gidevent" with gidEvent
            "gidannouncement" with gidAnnouncement
            "lang_list" with languages.ifEmpty { listOf(ELanguage.English) }.joinToString(separator = "_") { e -> e.ordinal.toString() }
            "origin" with "https://store.steampowered.com"
        }.body<NewsCalendarResponse>()

        return parseNewsEntries(
            entries = calendar.events
        )
    }

    suspend fun getMyVoteForAnnouncement(gid: String): NewsMyVote {
        return steamClient.webApi.store.method("events/ajaxgetmyannouncementvote/") {
            "gid" with gid
        }.body<NewsVoteResponse>().let { result ->
            NewsMyVote(
                isVotedUp = result.votedUp == 1,
                isVotedDown = result.votedDown == 1,
            )
        }
    }

    suspend fun setMyVoteForAnnouncement(gid: String, voteUp: Boolean) {
        // TODO
        // POST /updated/ajaxrateupdate/_ HTTP/1.1
        // MIME Type: application/x-www-form-urlencoded;charset=utf-8
        // sessionid: _
        // voteup: 0/1
        // clanid: _
        // ajax: 1
    }

    private suspend fun parseNewsEntries(
        entries: List<NewsEntry>,
        appsSourceMap: Map<AppId, NewsCalendarResponseApp> = emptyMap(),
        clanSourceMap: Map<Int, NewsCalendarResponseClan> = emptyMap(),
    ): List<NewsEvent> {
        val appsMap = mutableScatterMapOf<AppId, SteamApplication>()
        val clansMap = mutableIntObjectMapOf<ClanSummary>()

        val appsToLoad = mutableListOf<Int>()
        val clansToLoad = mutableListOf<Int>()

        if (appsSourceMap.isNotEmpty() && clanSourceMap.isNotEmpty()) {
            appsToLoad += appsSourceMap.keys.map(AppId::value)
            clansToLoad += clanSourceMap.keys
        } else {
            for (entry in entries) {
                if (entry.appid != 0) {
                    appsToLoad += entry.appid
                }

                if (entry.appid == 0 && entry.clanSteamid.isNotEmpty()) {
                    clansToLoad += entry.clanSteamid.toULong().toSteamId().accountId
                }
            }
        }

        measure("parseNewsEntries:requestSteamApplications") {
            for (app in steamClient.store.querySteamApplications(appsToLoad.distinct().map(::AppId))) {
                appsMap[app.id] = app
            }
        }

        measure("parseNewsEntries:requestClanSummaries") {
            clansToLoad.forEach { clanId ->
                clansMap[clanId.toInt()] = resolveClanInfo(clanId.toInt())
            }
        }

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

            val newsApp = appsSourceMap[AppId(entry.appid)]
            val newsPsMask = newsApp?.source ?: clanSourceMap[clanSteamid.accountId]?.source ?: 0

            val postSource = NewsEvent.PostSource(
                isFollowed = (newsPsMask and SOURCE_FOLLOWING) != 0,
                isRecommended = (newsPsMask and SOURCE_RECOMMENDED) != 0,
                isRequired = (newsPsMask and SOURCE_REQUIRED) != 0,
                isFeatured = (newsPsMask and SOURCE_FEATURED) != 0,
                isCurator = (newsPsMask and SOURCE_CURATOR) != 0,
                isRepost = (newsPsMask and SOURCE_REPOSTED) != 0,
                isInWishlist = (newsPsMask and SOURCE_WISHLIST) != 0,
                addedToWishlist = newsApp?.wishlistAdded?.toLong()?.let(Instant::fromEpochSeconds),
                isInLibrary = (newsPsMask and SOURCE_LIBRARY) != 0,
                playtimeTotal = newsApp?.playtime?.toDuration(DurationUnit.SECONDS),
                playtimeTwoWeeks = newsApp?.playtimeTwoWeeks?.toDuration(DurationUnit.SECONDS),
                lastPlayed = newsApp?.lastPlayed?.toLong()?.let(Instant::fromEpochSeconds),
            )

            return@map NewsEvent(
                id = entry.gid,
                announcementId = entry.announcementBody.gid,
                type = NewsEventType.entries.getOrElse(entry.eventType) { NewsEventType.Unknown },
                clanSteamId = SteamId(entry.clanSteamid.toULong()),
                creatorSteamId = SteamId(entry.creatorSteamid.toULong()),
                updaterSteamId = SteamId(entry.lastUpdateSteamid.toULong()),
                title = entry.eventName,
                subtitle = jsonDescription.subtitles.firstOrNull().orEmpty(),
                description = jsonDescription.summaries.firstOrNull().orEmpty(),
                header = headerImage,
                capsule = jsonDescription.capsuleImages.firstOrNull().orEmpty(),
                likeCount = entry.votesUp,
                dislikeCount = entry.votesDown,
                commentCount = entry.commentCount,
                forumTopicId = entry.forumTopicId,
                publishedAt = Instant.fromEpochSeconds(entry.announcementBody.posttime.toLong()),
                lastUpdatedAt = Instant.fromEpochSeconds(entry.announcementBody.updatetime.toLong()),
                relatedApp = appsMap[AppId(entry.appid)],
                clanSummary = clansMap[clanSteamid.accountId],
                content = entry.announcementBody.body,
                eventStartDate = Instant.fromEpochSeconds(entry.rtime32StartTime.toLong()),
                eventEndDate = Instant.fromEpochSeconds(entry.rtime32EndTime.toLong()),
                postSource = postSource
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
        accountId: Int
    ): ClanSummary {
        return clanCache.getOrPut(accountId) {
            steamClient.webApi.community.method("gid/$accountId/ajaxgetvanityandclanid/").body<ClanSummary>()
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