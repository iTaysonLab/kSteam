package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EAppFeature
import bruhcollective.itaysonlab.ksteam.models.enums.EGenre
import bruhcollective.itaysonlab.ksteam.models.enums.EPartner
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory
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
        byPartner = DfEntry((if (masterSubPackageId == 1289670) listOf(EPartner.EASubscription) else emptyList<EPartner>()) to false),
        byFriend = DfEntry(emptyList<SteamId>() to false)
    )
}

private fun KsLibraryQuery.toSteamStoreTags(): Pair<List<EGenre>, List<Int>> {
    val genres = mutableListOf<EGenre>()
    val tags = mutableListOf<Int>()

    for (tag in storeTags) {
        EGenre.byNumber(tag)?.let { genre -> genres.add(genre) } ?: tags.add(tag)
    }

    return genres to tags
}

private fun KsLibraryQuery.toSteamDynamicFeatures(): List<EAppFeature> {
    val list = mutableListOf<EAppFeature>()

    when (controllerSupport) {
        KsLibraryQueryControllerSupportFilter.None -> Unit
        KsLibraryQueryControllerSupportFilter.Partial -> list += EAppFeature.PartialControllerSupport
        KsLibraryQueryControllerSupportFilter.Full -> list += EAppFeature.FullControllerSupport
    }

    when (steamDeckMinimumSupport) {
        ESteamDeckSupport.Unknown -> Unit
        ESteamDeckSupport.Unsupported -> list += EAppFeature.SteamDeckUnsupported
        ESteamDeckSupport.Playable -> list += EAppFeature.SteamDeckPlayable
        ESteamDeckSupport.Verified -> list += EAppFeature.SteamDeckVerified
    }

    for (category in storeCategories.flatten()) {
        when (category) {
            EStoreCategory.Reserved0 -> Unit
            EStoreCategory.Multiplayer -> list += EAppFeature.MultiPlayer
            EStoreCategory.Singleplayer -> list += EAppFeature.SinglePlayer
            EStoreCategory.Reserved3 -> Unit
            EStoreCategory.Reserved4 -> Unit
            EStoreCategory.Reserved5 -> Unit
            EStoreCategory.ModHL2 -> Unit
            EStoreCategory.ModHL -> Unit
            EStoreCategory.VAC -> Unit
            EStoreCategory.Coop -> list += EAppFeature.CoOp
            EStoreCategory.Demo -> Unit
            EStoreCategory.Friends -> Unit
            EStoreCategory.HDR -> Unit
            EStoreCategory.CC -> Unit
            EStoreCategory.Commentary -> Unit
            EStoreCategory.Stats -> Unit
            EStoreCategory.SDK -> Unit
            EStoreCategory.Editor -> Unit
            EStoreCategory.PartialController -> list += EAppFeature.PartialControllerSupport
            EStoreCategory.Mod -> Unit
            EStoreCategory.MMO -> list += EAppFeature.MultiPlayer
            EStoreCategory.DLC -> Unit
            EStoreCategory.Achievements -> list += EAppFeature.Achievements
            EStoreCategory.Cloud -> list += EAppFeature.Cloud
            EStoreCategory.SharedSplitscreen -> list += EAppFeature.MultiPlayer
            EStoreCategory.Leaderboards -> Unit
            EStoreCategory.Guide -> Unit
            EStoreCategory.CrossPlatMultiplayer -> list += EAppFeature.MultiPlayer
            EStoreCategory.FullController -> list += EAppFeature.FullControllerSupport
            EStoreCategory.TradingCard -> list += EAppFeature.TradingCards
            EStoreCategory.Workshop -> list += EAppFeature.Workshop
            EStoreCategory.VRThirdParty -> list += EAppFeature.VRSupport
            EStoreCategory.AsyncGameNotifications -> Unit
            EStoreCategory.SteamController -> Unit
            EStoreCategory.VRSteam -> list += EAppFeature.VRSupport
            EStoreCategory.InAppPurchases -> Unit
            EStoreCategory.OnlinePvP -> list += EAppFeature.MultiPlayer
            EStoreCategory.LocalPvP -> list += EAppFeature.MultiPlayer
            EStoreCategory.OnlineCoop -> list += EAppFeature.CoOp
            EStoreCategory.LocalCoop -> list += EAppFeature.CoOp
            EStoreCategory.SteamVRCollectibles -> Unit
            EStoreCategory.RemotePlayToPhone -> list += EAppFeature.RemotePlayTogether
            EStoreCategory.RemotePlayToTablet -> list += EAppFeature.RemotePlayTogether
            EStoreCategory.RemotePlayToTV -> list += EAppFeature.RemotePlayTogether
            EStoreCategory.RemotePlayTogether -> list += EAppFeature.RemotePlayTogether
            EStoreCategory.CloudGaming -> Unit
            EStoreCategory.CloudGamingNVIDIA -> Unit
            EStoreCategory.LANPvP -> list += EAppFeature.MultiPlayer
            EStoreCategory.LANCoop -> list += EAppFeature.CoOp
            EStoreCategory.PvP -> list += EAppFeature.MultiPlayer
            EStoreCategory.HighQualitySoundtrackAudio -> Unit
            EStoreCategory.SteamChinaWorkshop -> Unit
            EStoreCategory.TrackedControllerSupport -> Unit
            EStoreCategory.VRSupported -> list += EAppFeature.VRSupport
            EStoreCategory.VROnly -> Unit
            EStoreCategory.PS4ControllerSupport -> list += EAppFeature.PS4ControllerSupport
            EStoreCategory.PS4ControllerBTSupport -> list += EAppFeature.PS4ControllerBTSupport
            EStoreCategory.PS5ControllerSupport -> list += EAppFeature.PS5ControllerSupport
            EStoreCategory.PS5ControllerBTSupport -> list += EAppFeature.PS5ControllerBTSupport
            EStoreCategory.SteamInputAPI -> list += EAppFeature.SteamInputAPI
            EStoreCategory.GamepadPreferred -> list += EAppFeature.GamepadPreferred
            EStoreCategory.HDROutput -> list += EAppFeature.HDR
            EStoreCategory.FamilySharing -> list += EAppFeature.FamilySharing
            EStoreCategory.SteamTimeline -> Unit
        }
    }

    return list.distinct()
}
