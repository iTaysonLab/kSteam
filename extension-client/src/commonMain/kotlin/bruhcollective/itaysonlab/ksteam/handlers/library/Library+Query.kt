package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.library.DfEntry
import bruhcollective.itaysonlab.ksteam.models.library.DynamicFilters
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
            ECollectionAppFeature.Ignored -> {}

            ECollectionAppFeature.FullControllerSupport -> {
                query.withControllerSupport(KsLibraryQueryControllerSupportFilter.Full)
            }

            ECollectionAppFeature.PartialControllerSupport -> {
                query.withControllerSupport(KsLibraryQueryControllerSupportFilter.Partial)
            }

            ECollectionAppFeature.VRSupport -> {
                query.withStoreCategories(
                    EStoreCategory.VRThirdParty,
                    EStoreCategory.VRSteam,
                    EStoreCategory.VRSupported
                )
            }

            ECollectionAppFeature.TradingCards -> {
                query.withStoreCategory(EStoreCategory.TradingCard)
            }

            ECollectionAppFeature.Workshop -> {
                query.withStoreCategory(EStoreCategory.Workshop)
            }

            ECollectionAppFeature.Achievements -> {
                query.withStoreCategory(EStoreCategory.Achievements)
            }

            ECollectionAppFeature.SinglePlayer -> {
                query.withStoreCategory(EStoreCategory.Singleplayer)
            }

            ECollectionAppFeature.MultiPlayer -> {
                query.withStoreCategories(
                    EStoreCategory.Multiplayer,
                    EStoreCategory.OnlinePvP,
                    EStoreCategory.LocalPvP,
                    EStoreCategory.CrossPlatMultiplayer,
                    EStoreCategory.MMO,
                    EStoreCategory.SharedSplitscreen
                )
            }

            ECollectionAppFeature.CoOp -> {
                query.withStoreCategories(
                    EStoreCategory.Coop,
                    EStoreCategory.OnlineCoop,
                    EStoreCategory.LocalCoop
                )
            }

            ECollectionAppFeature.Cloud -> {
                query.withStoreCategory(EStoreCategory.Cloud)
            }

            ECollectionAppFeature.RemotePlayTogether -> {
                query.withStoreCategory(EStoreCategory.RemotePlayTogether)
            }

            ECollectionAppFeature.SteamDeckVerified -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Verified)
            }

            ECollectionAppFeature.SteamDeckPlayable -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Playable)
            }

            ECollectionAppFeature.SteamDeckUnsupported -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Unsupported)
            }

            ECollectionAppFeature.SteamDeckUnknown -> {
                query.withSteamDeckMinimumSupport(ESteamDeckSupport.Unknown)
            }

            ECollectionAppFeature.PS4ControllerSupport -> {
                query.withStoreCategories(EStoreCategory.PS4ControllerSupport, EStoreCategory.PS4ControllerBTSupport)
            }

            ECollectionAppFeature.PS4ControllerBTSupport -> {
                query.withStoreCategory(EStoreCategory.PS4ControllerBTSupport)
            }

            ECollectionAppFeature.PS5ControllerSupport -> {
                query.withStoreCategories(EStoreCategory.PS5ControllerSupport, EStoreCategory.PS5ControllerBTSupport)
            }

            ECollectionAppFeature.PS5ControllerBTSupport -> {
                query.withStoreCategory(EStoreCategory.PS5ControllerBTSupport)
            }

            ECollectionAppFeature.SteamInputAPI -> {
                query.withStoreCategory(EStoreCategory.SteamInputAPI)
            }

            ECollectionAppFeature.GamepadPreferred -> {
                query.withStoreCategory(EStoreCategory.GamepadPreferred)
            }

            ECollectionAppFeature.HDR -> {
                query.withStoreCategory(EStoreCategory.HDROutput)
            }

            ECollectionAppFeature.FamilySharing -> {
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
            ECollectionPartner.EASubscription -> {
                query.withMasterSubscriptionPackage(1289670)
            }
        }
    }

    return query.build()
}

//

/**
 * Converts a kSteam library query to [DynamicFilters].
 */
fun KsLibraryQuery.toSteamDynamicCollection(): DynamicFilters {
    val (genres, storeTags) = toSteamStoreTags()

    return DynamicFilters(
        byAppType = DfEntry(appType to false),
        byPlayState = DfEntry(playState?.let { listOf(it) }.orEmpty() to false),
        byAppFeature = DfEntry(toSteamDynamicFeatures() to false),
        byGenre = DfEntry(genres to false),
        byStoreTag = DfEntry(storeTags to false),
        byPartner = DfEntry((if (masterSubPackageId == 1289670) listOf(ECollectionPartner.EASubscription) else emptyList<ECollectionPartner>()) to false),
        byFriend = DfEntry(emptyList<SteamId>() to false)
    )
}

private fun KsLibraryQuery.toSteamStoreTags(): Pair<List<ECollectionGenre>, List<Int>> {
    val genres = mutableListOf<ECollectionGenre>()
    val tags = mutableListOf<Int>()

    for (tag in storeTags) {
        ECollectionGenre.byNumber(tag)?.let { genre -> genres.add(genre) } ?: tags.add(tag)
    }

    return genres to tags
}

private fun KsLibraryQuery.toSteamDynamicFeatures(): List<ECollectionAppFeature> {
    val list = mutableListOf<ECollectionAppFeature>()

    when (controllerSupport) {
        KsLibraryQueryControllerSupportFilter.None -> Unit
        KsLibraryQueryControllerSupportFilter.Partial -> list += ECollectionAppFeature.PartialControllerSupport
        KsLibraryQueryControllerSupportFilter.Full -> list += ECollectionAppFeature.FullControllerSupport
    }

    when (steamDeckMinimumSupport) {
        ESteamDeckSupport.Unknown -> Unit
        ESteamDeckSupport.Unsupported -> list += ECollectionAppFeature.SteamDeckUnsupported
        ESteamDeckSupport.Playable -> list += ECollectionAppFeature.SteamDeckPlayable
        ESteamDeckSupport.Verified -> list += ECollectionAppFeature.SteamDeckVerified
    }

    for (category in storeCategories.flatten()) {
        when (category) {
            EStoreCategory.Reserved0 -> Unit
            EStoreCategory.Multiplayer -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.Singleplayer -> list += ECollectionAppFeature.SinglePlayer
            EStoreCategory.Reserved3 -> Unit
            EStoreCategory.Reserved4 -> Unit
            EStoreCategory.Reserved5 -> Unit
            EStoreCategory.ModHL2 -> Unit
            EStoreCategory.ModHL -> Unit
            EStoreCategory.VAC -> Unit
            EStoreCategory.Coop -> list += ECollectionAppFeature.CoOp
            EStoreCategory.Demo -> Unit
            EStoreCategory.Friends -> Unit
            EStoreCategory.HDR -> Unit
            EStoreCategory.CC -> Unit
            EStoreCategory.Commentary -> Unit
            EStoreCategory.Stats -> Unit
            EStoreCategory.SDK -> Unit
            EStoreCategory.Editor -> Unit
            EStoreCategory.PartialController -> list += ECollectionAppFeature.PartialControllerSupport
            EStoreCategory.Mod -> Unit
            EStoreCategory.MMO -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.DLC -> Unit
            EStoreCategory.Achievements -> list += ECollectionAppFeature.Achievements
            EStoreCategory.Cloud -> list += ECollectionAppFeature.Cloud
            EStoreCategory.SharedSplitscreen -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.Leaderboards -> Unit
            EStoreCategory.Guide -> Unit
            EStoreCategory.CrossPlatMultiplayer -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.FullController -> list += ECollectionAppFeature.FullControllerSupport
            EStoreCategory.TradingCard -> list += ECollectionAppFeature.TradingCards
            EStoreCategory.Workshop -> list += ECollectionAppFeature.Workshop
            EStoreCategory.VRThirdParty -> list += ECollectionAppFeature.VRSupport
            EStoreCategory.AsyncGameNotifications -> Unit
            EStoreCategory.SteamController -> Unit
            EStoreCategory.VRSteam -> list += ECollectionAppFeature.VRSupport
            EStoreCategory.InAppPurchases -> Unit
            EStoreCategory.OnlinePvP -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.LocalPvP -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.OnlineCoop -> list += ECollectionAppFeature.CoOp
            EStoreCategory.LocalCoop -> list += ECollectionAppFeature.CoOp
            EStoreCategory.SteamVRCollectibles -> Unit
            EStoreCategory.RemotePlayToPhone -> list += ECollectionAppFeature.RemotePlayTogether
            EStoreCategory.RemotePlayToTablet -> list += ECollectionAppFeature.RemotePlayTogether
            EStoreCategory.RemotePlayToTV -> list += ECollectionAppFeature.RemotePlayTogether
            EStoreCategory.RemotePlayTogether -> list += ECollectionAppFeature.RemotePlayTogether
            EStoreCategory.CloudGaming -> Unit
            EStoreCategory.CloudGamingNVIDIA -> Unit
            EStoreCategory.LANPvP -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.LANCoop -> list += ECollectionAppFeature.CoOp
            EStoreCategory.PvP -> list += ECollectionAppFeature.MultiPlayer
            EStoreCategory.HighQualitySoundtrackAudio -> Unit
            EStoreCategory.SteamChinaWorkshop -> Unit
            EStoreCategory.TrackedControllerSupport -> Unit
            EStoreCategory.VRSupported -> list += ECollectionAppFeature.VRSupport
            EStoreCategory.VROnly -> Unit
            EStoreCategory.PS4ControllerSupport -> list += ECollectionAppFeature.PS4ControllerSupport
            EStoreCategory.PS4ControllerBTSupport -> list += ECollectionAppFeature.PS4ControllerBTSupport
            EStoreCategory.PS5ControllerSupport -> list += ECollectionAppFeature.PS5ControllerSupport
            EStoreCategory.PS5ControllerBTSupport -> list += ECollectionAppFeature.PS5ControllerBTSupport
            EStoreCategory.SteamInputAPI -> list += ECollectionAppFeature.SteamInputAPI
            EStoreCategory.GamepadPreferred -> list += ECollectionAppFeature.GamepadPreferred
            EStoreCategory.HDROutput -> list += ECollectionAppFeature.HDR
            EStoreCategory.FamilySharing -> list += ECollectionAppFeature.FamilySharing
            EStoreCategory.SteamTimeline -> Unit
        }
    }

    return list.distinct()
}
