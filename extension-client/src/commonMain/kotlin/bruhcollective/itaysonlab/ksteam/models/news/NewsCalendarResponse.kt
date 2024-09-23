package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.Serializable

@Serializable
internal data class NewsCalendarResponse (
    val success: Int,
    val apps: List<NewsCalendarResponseApp> = emptyList(),
    val backwardComplete: Boolean = true,
    val clans: List<NewsCalendarResponseClan> = emptyList(),
    val documents: List<NewsDocument> = emptyList(),
    val events: List<NewsEntry> = emptyList()
)