package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.models.enums.EAppFeature
import bruhcollective.itaysonlab.ksteam.models.enums.EPartner
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory
import bruhcollective.itaysonlab.ksteam.models.library.LibraryCollection
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQuery
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQueryBuilder
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQueryControllerSupportFilter

/**
 * Converts [LibraryCollection.Dynamic] to a kSteam library query.
 */
fun LibraryCollection.Dynamic.toKsLibraryQuery(): KsLibraryQuery {
    val query = KsLibraryQueryBuilder()

    // 1. byAppType
    for (appType in filters.byAppType.entries) {
        query.withAppType(appType)
    }

    // 2. byPlayState
    filters.byPlayState.entries.firstOrNull()?.let { playState ->
        query.withPlayState(playState)
    }

    // 3. byAppFeature
    for (feature in filters.byAppFeature.entries) {
        when (feature) {
            EAppFeature.Ignored -> {}

            EAppFeature.FullControllerSupport -> {
                query.withControllerSupport(KsLibraryQueryControllerSupportFilter.Full)
            }

            EAppFeature.PartialControllerSupport -> {
                query.withControllerSupport(KsLibraryQueryControllerSupportFilter.Partial)
            }

            EAppFeature.VRSupport -> {
                query.withStoreCategories(
                    EStoreCategory.VRThirdParty,
                    EStoreCategory.VRSteam,
                    EStoreCategory.VRSupported
                )
            }

            EAppFeature.TradingCards -> {
                query.withStoreCategory(EStoreCategory.TradingCard)
            }

            EAppFeature.Workshop -> {
                query.withStoreCategory(EStoreCategory.Workshop)
            }

            EAppFeature.Achievements -> {
                query.withStoreCategory(EStoreCategory.Achievements)
            }

            EAppFeature.SinglePlayer -> {
                query.withStoreCategory(EStoreCategory.Singleplayer)
            }

            EAppFeature.MultiPlayer -> {
                query.withStoreCategories(
                    EStoreCategory.Multiplayer,
                    EStoreCategory.OnlinePvP,
                    EStoreCategory.LocalPvP,
                    EStoreCategory.CrossPlatMultiplayer,
                    EStoreCategory.MMO,
                    EStoreCategory.SharedSplitscreen
                )
            }

            EAppFeature.CoOp -> {
                query.withStoreCategories(
                    EStoreCategory.Coop,
                    EStoreCategory.OnlineCoop,
                    EStoreCategory.LocalCoop
                )
            }

            EAppFeature.Cloud -> {
                query.withStoreCategory(EStoreCategory.Cloud)
            }

            EAppFeature.RemotePlayTogether -> {
                query.withStoreCategory(EStoreCategory.RemotePlayTogether)
            }

            EAppFeature.SteamDeckVerified -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Verified)
            }

            EAppFeature.SteamDeckPlayable -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Playable)
            }

            EAppFeature.SteamDeckUnsupported -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Unsupported)
            }

            EAppFeature.SteamDeckUnknown -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Unknown)
            }

            EAppFeature.PS4ControllerSupport -> {
                query.withStoreCategories(EStoreCategory.PS4ControllerSupport, EStoreCategory.PS4ControllerBTSupport)
            }

            EAppFeature.PS4ControllerBTSupport -> {
                query.withStoreCategory(EStoreCategory.PS4ControllerBTSupport)
            }

            EAppFeature.PS5ControllerSupport -> {
                query.withStoreCategories(EStoreCategory.PS5ControllerSupport, EStoreCategory.PS5ControllerBTSupport)
            }

            EAppFeature.PS5ControllerBTSupport -> {
                query.withStoreCategory(EStoreCategory.PS5ControllerBTSupport)
            }

            EAppFeature.SteamInputAPI -> {
                query.withStoreCategory(EStoreCategory.SteamInputAPI)
            }

            EAppFeature.GamepadPreferred -> {
                query.withStoreCategory(EStoreCategory.GamepadPreferred)
            }

            EAppFeature.HDR -> {
                query.withStoreCategory(EStoreCategory.HDROutput)
            }

            EAppFeature.FamilySharing -> {
                query.withStoreCategory(EStoreCategory.FamilySharing)
            }
        }
    }

    // 4. byGenre
    for (genre in filters.byGenre.entries) {
        query.withGenre(genre)
    }

    // 5. byStoreTag
    query.withStoreTags(filters.byStoreTag.entries)

    // 6. byPartner
    for (partner in filters.byPartner.entries) {
        when (partner) {
            EPartner.EASubscription -> {
                query.withMasterSubscriptionPackage(1289670)
            }
        }
    }

    return query.build()
}