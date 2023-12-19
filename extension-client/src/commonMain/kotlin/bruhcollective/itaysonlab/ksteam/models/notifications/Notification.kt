package bruhcollective.itaysonlab.ksteam.models.notifications

import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.econ.EconItemReference
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a notification on the Steam network.
 */
sealed interface Notification {
    /**
     * Timestamp, in seconds, when the notification was posted.
     */
    val timestamp: Int

    /**
     * Is notification unread.
     */
    val unread: Boolean

    /**
     * Is notification dismissed (swiped).
     */
    val hidden: Boolean

    /**
     * A user has received a gift from another user.
     */
    class Gift internal constructor(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val gifter: Flow<Persona>
    ) : Notification {
        @Serializable
        class Body(
            @SerialName("gifter_account") val accountId: Long = 0,
        )
    }

    /**
     * A user has received a new item(s) in their inventory (through gameplay, trading or buying).
     */
    class Item internal constructor(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val item: EconItemReference
    ) : Notification {
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
    class FriendRequest internal constructor(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val requestor: Flow<Persona>
    ) : Notification {
        @Serializable
        class Body(
            @SerialName("requestor_id") val accountId: Long = 0,
        )
    }

    /**
     * One (or multiple) games from a user's wishlish has received a major discount on a sale.
     */
    class WishlistSale internal constructor(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val appSummary: AppSummary?
    ) : Notification {
        val isMultipleItemsOnSale get() = appSummary == null

        @Serializable
        class Body(
            @SerialName("appid") val appId: Int = 0
        )
    }

    /**
     * A promotion message.
     */
    class Promotion internal constructor(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val title: String,
        val description: String,
        val iconUrl: String,
        val link: String
    ) : Notification {
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
    class Unknown internal constructor(
        override val timestamp: Int,
        override val unread: Boolean,
        override val hidden: Boolean,
        val rawJsonData: String
    ) : Notification
}