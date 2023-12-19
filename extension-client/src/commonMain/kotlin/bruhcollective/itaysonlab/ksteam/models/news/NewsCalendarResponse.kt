package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.Serializable

@Serializable
data class NewsCalendarResponse internal constructor(
    val success: Int,
    val apps: List<NewsCalendarResponseApp> = emptyList(),
    val backwardComplete: Boolean = true,
    val clans: List<NewsCalendarResponseClan> = emptyList(),
    val documents: List<NewsDocument> = emptyList(),
    val events: List<NewsEntry> = emptyList()
)