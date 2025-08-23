package bruhcollective.itaysonlab.ksteam.models.wishlist

import bruhcollective.itaysonlab.ksteam.models.AppId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class WishlistItem (
    val app: AppId,
    val priority: Int,
    val dateAdded: Instant?,
)