package bruhcollective.itaysonlab.ksteam.models.notifications

import bruhcollective.itaysonlab.ksteam.models.apps.App
import bruhcollective.itaysonlab.ksteam.models.econ.EconItemReference
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a notification on the Steam network.
 */
sealed class Notification {
    /**
     * Timestamp, in seconds, when the notification was posted.
     */
    abstract val timestamp: Int

    /**
     * Is notification unread.
     */
    abstract val unread: Boolean

    /**
     * Is notification dismissed (swiped).
     */
    abstract val hidden: Boolean

    /**
     * A user has received a gift from another user.
     */
    class Gift(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val gifter: Flow<Persona>
    ): Notification() {
        @Serializable
        class Body(
            @SerialName("gifter_account") val steamId: Long = 0,
        )
    }

    /**
     * A user has received a new item(s) in their inventory (through gameplay, trading or buying).
     */
    class Item(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val item: EconItemReference
    ): Notification() {
        @Serializable
        class Body(
            @SerialName("app_id") val appId: Int = 0,
            @SerialName("context_id") val contextId: Int = 0,
            @SerialName("asset_id") val assetId: Long = 0,
        )
    }

    /**
     * A user has received a friend request.
     */
    class FriendRequest(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val requestor: Flow<Persona>
    ): Notification() {
        @Serializable
        class Body(
            @SerialName("requestor_id") val steamId: Long = 0,
        )
    }

    /**
     * One (or multiple) games from a user's wishlish has received a major discount on a sale.
     */
    class WishlistSale(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val app: App?
    ): Notification() {
        val isMultipleItemsOnSale get() = app == null

        @Serializable
        class Body(
            @SerialName("appid") val appId: Int = 0
        )
    }

    /**
     * A promotion message.
     */
    class Promotion(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val title: String,
        val description: String,
        val iconUrl: String,
        val link: String
    ): Notification() {
        @Serializable
        class Body(
            @SerialName("title") val title: String = "",
            @SerialName("body") val body: String = "",
            @SerialName("image") val image: String = "",
            @SerialName("link") val link: String = "",
        )
    }

    /**
     * kSteam does not know about the notification type.
     *
     * Data is provided in raw form if you want to implement support before kSteam does.
     */
    class Unknown(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val rawJsonData: String
    ): Notification()
}