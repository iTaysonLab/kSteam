package bruhcollective.itaysonlab.ksteam.models.news

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Describes an event on the Steam "News" page.
 */
@Serializable
data class NewsEvent (
    /**
     * The unique ID of an event.
     */
    val id: String,
    val announcementId: String,

    /**
     * The type of the event
     */
    val type: NewsEventType,

    /**
     * The [SteamId] of a clan (group) which posted this.
     *
     * Might be hidden and not resolvable.
     */
    val clanSteamId: SteamId,

    /**
     * The [SteamId] of a user who created the event.
     *
     * Might be hidden and not resolvable.
     */
    val creatorSteamId: SteamId,

    /**
     * A short summary of a clan which posted this event.
     */
    val clanSummary: ClanSummary?,

    /**
     * The [SteamId] of a user who last updated the event.
     *
     * Might be hidden and not resolvable.
     */
    val updaterSteamId: SteamId,

    /**
     * Title of an event.
     */
    val title: String,

    /**
     * Subtitle of an event.
     *
     * For now, only English localization is returned.
     */
    val subtitle: String,

    /**
     * Description of an event.
     *
     * For now, only English localization is returned.
     */
    val description: String,

    /**
     * A URL to the post's header.
     *
     * For now, only English localization is returned.
     */
    val header: String,

    /**
     * A URL to the post's capsule.
     *
     * For now, only English localization is returned.
     */
    val capsule: String,

    /**
     * The amount of likes taken directly from event announcement.
     */
    val likeCount: Int,

    /**
     * The amount of dislikes taken directly from event announcement.
     */
    val dislikeCount: Int,

    /**
     * The amount of comments taken from the forum post.
     */
    val commentCount: Int,

    /**
     * ID of a forum topic where user can comment the event.
     */
    val forumTopicId: String,

    /**
     * Date when the event was posted.
     */
    val publishedAt: Instant,

    /**
     * Date when the event was updated.
     */
    val lastUpdatedAt: Instant,

    /**
     * Date when the event should start.
     */
    val eventStartDate: Instant,

    /**
     * Date when the event should end.
     */
    val eventEndDate: Instant,

    /**
     * A short description of an application linked to this event
     */
    val relatedApp: SteamApplication?,

    /**
     * A BB-Code like representation of a news content.
     */
    val content: String,

    /**
     * Source information.
     */
    val postSource: PostSource
) {
    @Serializable
    data class PostSource (
        val isFollowed: Boolean,
        val isRecommended: Boolean,
        val isRequired: Boolean,
        val isFeatured: Boolean,
        val isCurator: Boolean,
        val isRepost: Boolean,

        // Wishlist
        val isInWishlist: Boolean,
        val addedToWishlist: Instant?,

        // Library
        val isInLibrary: Boolean,
        val playtimeTotal: Duration?,
        val playtimeTwoWeeks: Duration?,
        val lastPlayed: Instant?
    )
}