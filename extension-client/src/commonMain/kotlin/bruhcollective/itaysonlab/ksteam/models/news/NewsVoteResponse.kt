package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NewsVoteResponse (
    val success: Int,
    @SerialName("voted_down") val votedDown: Int = 0,
    @SerialName("voted_up") val votedUp: Int = 0,
)