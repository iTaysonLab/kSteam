package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.models.enums.EAppFeature
import bruhcollective.itaysonlab.ksteam.models.enums.EGenre
import bruhcollective.itaysonlab.ksteam.models.enums.EPartner
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.library.DynamicFilters
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.types.BaseRealmObject

/**
 * Composes a Realm query by Steam Collection filter set.
 */
internal fun Pics.composeRealmQueryByFilters(filters: DynamicFilters): RealmQuery<AppInfo> {
    var realmQuery = database.sharedRealm.query<AppInfo>()

    // 1. byAppType
    if (filters.byAppType.entries.isNotEmpty()) {
        // acceptsUnion is useless here?
        realmQuery = realmQuery.query("common.type IN $0", filters.byAppType.entries.map { it.name })
    }

    // 2. byPlayState
    // PlayedPreviously/PlayedNever is handled in [Library] handler due to separate objects\
    // Other switches are not supported for kSteam use cases

    // 3. byAppFeature
    // TODO compact containsCategory calls into one large AND ALL {...} == common.category.@keys
    for (filter in filters.byAppFeature.entries) {
        realmQuery = when (filter) {
            EAppFeature.FullControllerSupport -> {
                realmQuery.query("common.controllerSupport ==[c] $0 OR common.category[$1] == TRUE", "full", "category_28")
            }

            EAppFeature.PartialControllerSupport -> {
                realmQuery.query("common.controllerSupport IN $0 OR common.category[$1] == TRUE OR common.category[$2] == TRUE", listOf("full", "partial"), "category_18", "category_28")
            }

            EAppFeature.VRSupport -> {
                realmQuery.containsCategory(31)
            }

            EAppFeature.TradingCards -> {
                realmQuery.containsCategory(29)
            }

            EAppFeature.Workshop -> {
                realmQuery.containsCategory(30)
            }

            EAppFeature.Achievements -> {
                realmQuery.containsCategory(22)
            }

            EAppFeature.SinglePlayer -> {
                realmQuery.containsCategory(2)
            }

            EAppFeature.MultiPlayer -> {
                realmQuery.containsAnyCategories(36, 37, 20, 24, 27, 1)
            }

            EAppFeature.CoOp -> {
                realmQuery.containsAnyCategories(9, 38, 39)
            }

            EAppFeature.Cloud -> {
                realmQuery.containsCategory(23)
            }

            EAppFeature.RemotePlayTogether -> {
                realmQuery.containsCategory(44)
            }

            EAppFeature.SteamDeckVerified -> {
                realmQuery.query("common.steamDeckCompat.category == $0", ESteamDeckSupport.Verified.ordinal)
            }

            EAppFeature.SteamDeckPlayable -> {
                realmQuery.query("common.steamDeckCompat.category IN $0", listOf(ESteamDeckSupport.Verified.ordinal, ESteamDeckSupport.Playable.ordinal))
            }

            EAppFeature.SteamDeckUnknown -> {
                realmQuery.query("common.steamDeckCompat.category != $0", ESteamDeckSupport.Unsupported.ordinal)
            }

            EAppFeature.SteamDeckUnsupported -> {
                // Just include all games, this literally includes ALL 4 SD support groups
                realmQuery
            }
        }
    }

    // 4. byGenre
    realmQuery = realmQuery.containsTags(filters.byGenre.acceptsUnion, filters.byGenre.entries.map(EGenre::tagNumber))

    // 5. byStoreTag
    realmQuery = realmQuery.containsTags(filters.byStoreTag.acceptsUnion, filters.byStoreTag.entries)

    // 6. byPartner
    for (filter in filters.byPartner.entries) {
        when (filter) {
            EPartner.EASubscription -> {
                realmQuery = realmQuery.query("common.masterSubPackageId == $0", 1289670) // EA Play package
            }
        }
    }

    return realmQuery
}

// like containsAnyCategories, but... more light?
private fun <T: BaseRealmObject> RealmQuery<T>.containsCategory(id: Int): RealmQuery<T> {
    return query("ANY common.category.@keys == $0", "category_$id")
}

private fun <T: BaseRealmObject> RealmQuery<T>.containsAnyCategories(vararg ids: Int): RealmQuery<T> {
    if (ids.isEmpty()) {
        return this
    }

    // ANY common.category.@keys IN {"category_9", "category_38", "category_39"}
    return query("ANY common.category.@keys IN $0", ids.map { id -> "category_$id" })
}

private fun <T: BaseRealmObject> RealmQuery<T>.containsTags(union: Boolean, ids: List<Int>): RealmQuery<T> {
    if (ids.isEmpty()) {
        return this
    }

    val keyword = if (union) {
        "ANY"
    } else {
        "ALL"
    }

    return query("$keyword $0 == common.tags.@keys", ids)
}
