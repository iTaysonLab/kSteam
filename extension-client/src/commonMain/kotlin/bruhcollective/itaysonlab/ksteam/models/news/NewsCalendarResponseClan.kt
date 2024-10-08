package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NewsCalendarResponseClan (
    val source: Int,
    @SerialName("clanid") val clanId: Long
)