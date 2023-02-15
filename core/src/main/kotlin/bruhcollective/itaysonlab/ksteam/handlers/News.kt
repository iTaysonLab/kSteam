package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.news.AppType
import bruhcollective.itaysonlab.ksteam.models.news.EventType
import bruhcollective.itaysonlab.ksteam.models.news.NewsCalendarResponse

/**
 * Access Steam news using this handler.
 */
class News(
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
        steamClient.externalWebApi.ajaxGetTyped<NewsCalendarResponse>(path = listOf("events", "ajaxgetusereventcalendarrange", ""), parameters = mapOf(

        ))
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit
}