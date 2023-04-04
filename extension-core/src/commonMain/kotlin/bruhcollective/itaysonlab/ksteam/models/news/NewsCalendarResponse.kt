package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.Serializable

@Serializable
data class NewsCalendarResponse internal constructor(
    val success: Int,
    val apps: List<bruhcollective.itaysonlab.ksteam.models.news.NewsCalendarResponseApp> = emptyList(),
    val backwardComplete: Boolean,
    val clans: List<bruhcollective.itaysonlab.ksteam.models.news.NewsCalendarResponseClan> = emptyList(),
    val documents: List<bruhcollective.itaysonlab.ksteam.models.news.NewsDocument> = emptyList(),
    val events: List<bruhcollective.itaysonlab.ksteam.models.news.NewsEntry> = emptyList()
)