package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsCalendarResponseApp (
    val source: Int,
    @SerialName("appid") val appId: Int,
    @SerialName("last_played") val lastPlayed: Int = 0,
    val playtime: Int = 0,
    @SerialName("playtime_2weeks") val playtimeTwoWeeks: Int = 0
)