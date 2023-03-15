package bruhcollective.itaysonlab.ksteam.models.news.community

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

object CommunityHubSerializer: JsonContentPolymorphicSerializer<bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost>(
    bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.intOrNull) {
            3 -> serializer<bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost.Artwork>()
            4 -> serializer<bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost.Video>()
            5 -> serializer<bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost.Screenshot>()
            else -> serializer<bruhcollective.itaysonlab.ksteam.models.news.community.CommunityHubPost.CommunityItem>()
        }
    }
}