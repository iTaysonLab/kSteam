package bruhcollective.itaysonlab.ksteam.models.news

import bruhcollective.itaysonlab.ksteam.models.news.EventType.News

/**
 * Event types you can request from [News].
 */
enum class EventType (
    internal val internalTypes: List<Int>
) {
    News(internalTypes = listOf(28)),
    Events(internalTypes = listOf(9, 27, 22, 23, 24, 35, 25, 26)),
    Streaming(internalTypes = listOf(11)),
    Updates(internalTypes = listOf(12, 13, 14)),
    Releases(internalTypes = listOf(10, 29, 16, 15, 32)),
    Sales(internalTypes = listOf(20, 21, 31, 34));
}