package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsCalendarResponseApp internal constructor(
    val source: Int,
    @SerialName("appid") val appId: Int,
    @SerialName("last_played") val lastPlayed: Int = 0,
    val playtime: Int = 0,
    @SerialName("playtime_2weeks") val playtimeTwoWeeks: Int = 0
) {
    internal enum class SourceFlags (val bitmask: Int) {
        Library(1),
        Wishlist(2),
        Following(4),
        Recommended(8),
        Steam(16),
        Required(32),
        Featured(64),
        Curator(128),
        Reposted(256);
    }
}