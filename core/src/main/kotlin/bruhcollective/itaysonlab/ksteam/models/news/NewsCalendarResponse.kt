package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.Serializable

@Serializable
data class NewsCalendarResponse (
    val success: Int,
    val apps: List<NewsCalendarResponseApp> = emptyList(),
    val backwardComplete: Boolean,
    val clans: List<NewsCalendarResponseClan> = emptyList(),
    val documents: List<NewsDocument> = emptyList(),
    val events: List<NewsEntry> = emptyList()
)