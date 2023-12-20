package bruhcollective.itaysonlab.ksteam.models.news.community

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

object CommunityHubSerializer: JsonContentPolymorphicSerializer<CommunityHubPost>(CommunityHubPost::class) {
    @Suppress("REDUNDANT_PROJECTION")
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out CommunityHubPost> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.intOrNull) {
            3 -> serializer<CommunityHubPost.Artwork>()
            4 -> serializer<CommunityHubPost.Video>()
            5 -> serializer<CommunityHubPost.Screenshot>()
            else -> serializer<CommunityHubPost.CommunityItem>()
        }
    }
}