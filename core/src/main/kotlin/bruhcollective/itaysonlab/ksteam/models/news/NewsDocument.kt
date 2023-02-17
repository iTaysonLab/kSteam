package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsDocument internal constructor(
    @SerialName("start_time") val startTime: Int,
    @SerialName("unique_id") val uniqueId: String,
    @SerialName("appid") val appId: Int,
    @SerialName("clanid") val clanId: Int,
    @SerialName("event_type") val eventType: Int,
    val score: Int
)