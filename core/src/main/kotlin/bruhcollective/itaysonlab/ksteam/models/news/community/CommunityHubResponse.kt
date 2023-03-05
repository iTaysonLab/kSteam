package bruhcollective.itaysonlab.ksteam.models.news.community

import kotlinx.serialization.Serializable

@Serializable
class CommunityHubResponse (
    val cached: Boolean,
    val hub: List<CommunityHubPost>
)