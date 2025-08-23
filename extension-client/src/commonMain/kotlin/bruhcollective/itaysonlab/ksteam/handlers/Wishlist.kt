package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.wishlist.WishlistItem
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import steam.webui.wishlist.CWishlist_AddToWishlist_Request
import steam.webui.wishlist.CWishlist_GetWishlistItemCount_Request
import steam.webui.wishlist.CWishlist_GetWishlist_Request
import steam.webui.wishlist.CWishlist_RemoveFromWishlist_Request
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Access Steam friend activity using this handler.
 */
class Wishlist internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    @OptIn(ExperimentalTime::class)
    suspend fun getWishlist(steamId: SteamId = steamClient.currentSessionSteamId): List<WishlistItem> {
        return steamClient.grpc.wishlist.GetWishlist().executeSteam(
            data = CWishlist_GetWishlist_Request(steamid = steamId.longId)
        ).items.mapNotNull { obj ->
            WishlistItem(
                app = obj.appid?.let(::AppId) ?: return@mapNotNull null,
                priority = obj.priority ?: 0,
                dateAdded = obj.date_added?.let { sec -> Instant.fromEpochSeconds(sec.toLong()) },
            )
        }
    }

    suspend fun addToWishlist(app: AppId): Int {
        return steamClient.grpc.wishlist.AddToWishlist().executeSteam(
            data = CWishlist_AddToWishlist_Request(
                appid = app.value,
                navdata = null // TODO: figure this??
            )
        ).wishlist_count ?: 0
    }

    suspend fun removeFromWishlist(app: AppId): Int {
        return steamClient.grpc.wishlist.RemoveFromWishlist().executeSteam(
            data = CWishlist_RemoveFromWishlist_Request(
                appid = app.value
            )
        ).wishlist_count ?: 0
    }

    suspend fun getWishlistItemCount(steamId: SteamId = steamClient.currentSessionSteamId): Int {
        return steamClient.grpc.wishlist.GetWishlistItemCount().executeSteam(
            data = CWishlist_GetWishlistItemCount_Request(steamid = steamId.longId)
        ).count ?: 0
    }

    suspend fun getWishlistItemsOnSale() {
        TODO()
        /*steamClient.grpc.wishlist.GetWishlistItemsOnSale().executeSteam(
            data = CWishlist_GetWishlistItemsOnSale_Request()
        )*/
    }

    suspend fun getWishlistSortedFiltered() {
        TODO()
    }

    //

    suspend fun isAddedToWishlist(app: AppId): Boolean {
        // TODO: maybe cache the wishlist for fast comparison?
        return getWishlist(steamClient.currentSessionSteamId).any { it.app == app }
    }
}