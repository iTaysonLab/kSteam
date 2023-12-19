package bruhcollective.itaysonlab.ksteam.models.news.community

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = CommunityHubSerializer::class)
@Immutable
sealed class CommunityHubPost {
    @SerialName("published_file_id")
    abstract val fileId: String

    @SerialName("title")
    abstract val title: String

    @SerialName("preview_image_url")
    abstract val previewImage: String

    @SerialName("full_image_url")
    abstract val fullImage: String

    @SerialName("image_width")
    abstract val imageWidth: Int

    @SerialName("image_height")
    abstract val imageHeight: Int

    @SerialName("comment_count")
    abstract val commentCount: Int

    @SerialName("votes_for")
    abstract val votes: Int

    @SerialName("rating_stars")
    abstract val starRating: Int

    @SerialName("description")
    abstract val description: String

    @SerialName("reactions")
    abstract val reactions: List<CommunityHubReactionItem>

    @SerialName("creator")
    abstract val creator: CommunityHubPersonaInformation

    @Serializable
    @Immutable
    data class Artwork(
        @SerialName("published_file_id") override val fileId: String,
        @SerialName("title") override val title: String,
        @SerialName("preview_image_url") override val previewImage: String,
        @SerialName("full_image_url") override val fullImage: String,
        @SerialName("image_width") override val imageWidth: Int,
        @SerialName("image_height") override val imageHeight: Int,
        @SerialName("comment_count") override val commentCount: Int,
        @SerialName("votes_for") override val votes: Int,
        @SerialName("rating_stars") override val starRating: Int,
        @SerialName("description") override val description: String,
        @SerialName("reactions") override val reactions: List<CommunityHubReactionItem>,
        @SerialName("creator") override val creator: CommunityHubPersonaInformation,
    ): CommunityHubPost()

    @Serializable
    @Immutable
    data class Video(
        @SerialName("published_file_id") override val fileId: String,
        @SerialName("title") override val title: String,
        @SerialName("preview_image_url") override val previewImage: String,
        @SerialName("full_image_url") override val fullImage: String,
        @SerialName("image_width") override val imageWidth: Int,
        @SerialName("image_height") override val imageHeight: Int,
        @SerialName("comment_count") override val commentCount: Int,
        @SerialName("votes_for") override val votes: Int,
        @SerialName("rating_stars") override val starRating: Int,
        @SerialName("description") override val description: String,
        @SerialName("reactions") override val reactions: List<CommunityHubReactionItem>,
        @SerialName("creator") override val creator: CommunityHubPersonaInformation,
        @SerialName("youtube_video_id") val youtubeVideoId: String
    ): CommunityHubPost()

    @Serializable
    @Immutable
    data class Screenshot(
        @SerialName("published_file_id") override val fileId: String,
        @SerialName("title") override val title: String,
        @SerialName("preview_image_url") override val previewImage: String,
        @SerialName("full_image_url") override val fullImage: String,
        @SerialName("image_width") override val imageWidth: Int,
        @SerialName("image_height") override val imageHeight: Int,
        @SerialName("comment_count") override val commentCount: Int,
        @SerialName("votes_for") override val votes: Int,
        @SerialName("rating_stars") override val starRating: Int,
        @SerialName("description") override val description: String,
        @SerialName("reactions") override val reactions: List<CommunityHubReactionItem>,
        @SerialName("creator") override val creator: CommunityHubPersonaInformation,
    ): CommunityHubPost()

    @Serializable
    @Immutable
    data class CommunityItem(
        @SerialName("published_file_id") override val fileId: String,
        @SerialName("title") override val title: String,
        @SerialName("preview_image_url") override val previewImage: String,
        @SerialName("full_image_url") override val fullImage: String,
        @SerialName("image_width") override val imageWidth: Int,
        @SerialName("image_height") override val imageHeight: Int,
        @SerialName("comment_count") override val commentCount: Int,
        @SerialName("votes_for") override val votes: Int,
        @SerialName("rating_stars") override val starRating: Int,
        @SerialName("description") override val description: String,
        @SerialName("reactions") override val reactions: List<CommunityHubReactionItem>,
        @SerialName("creator") override val creator: CommunityHubPersonaInformation,
    ): CommunityHubPost()
}

@Serializable
@Immutable
class CommunityHubPersonaInformation(
    val name: String,
    val steamid: String,
    val avatar: String
) {
    val steamId get() = SteamId(steamid.toULong())
}

@Serializable
@Immutable
class CommunityHubReactionItem(
    @SerialName("reaction_type") val reactionId: Int,
    val count: Int
)