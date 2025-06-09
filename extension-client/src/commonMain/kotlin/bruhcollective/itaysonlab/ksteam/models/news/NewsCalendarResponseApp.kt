package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NewsCalendarResponseApp (
    val source: Int,
    @SerialName("appid") val appId: Int,
    @SerialName("last_played") val lastPlayed: Int = 0,
    @SerialName("playtime") val playtime: Int = 0,
    @SerialName("playtime_2weeks") val playtimeTwoWeeks: Int = 0,
    @SerialName("wishlist_added") val wishlistAdded: Int = 0,
)