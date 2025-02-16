package bruhcollective.itaysonlab.ksteam.models.app

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.*
import bruhcollective.itaysonlab.ksteam.database.room.entity.store.RoomStoreTag
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.Assets.LocalizedAssetPack.RetinaAsset
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckTestResult
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import steam.webui.common.StoreItem
import steam.webui.common.StoreItem_BasicInfo_CreatorHomeLink
import steam.webui.common.StoreItem_Tag

/**
 * Converts objects into [SteamApplication].
 */
internal object SteamApplicationFactory {
    /**
     * Converts a database [bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.RoomPicsFullAppInfo] into a client-facing [SteamApplication].
     */
    internal fun fromDatabase(info: RoomPicsFullAppInfo): SteamApplication {
        return fromDatabase(
            info = info.appInfo,
            categories = info.categories,
            tags = info.tags,
            descriptors = info.descriptors,
            localizedAssets = info.localizedAssets,
            associations = info.associations,
            liteObject = false
        )
    }

    /**
     * Converts an online [steam.webui.common.StoreItem] into a client-facing [SteamApplication].
     */
    internal fun fromStoreItem(
        info: StoreItem
    ): SteamApplication {
        return SteamApplication(
            isLiteObject = false,
            id = AppId(info.appid!!),
            type = when (info.type) {
                // TODO
                4 -> SteamApplication.Type.DLC
                else -> SteamApplication.Type.Game
            },
            name = info.name.orEmpty(),
            steamReleaseDate = info.release?.steam_release_date?.toLong() ?: 0L,
            originalReleaseDate = 0L,
            supportedOs = emptyList(),
            releaseState = when {
                info.release?.is_preload == true -> SteamApplication.ReleaseState.PreloadOnly
                info.release?.is_coming_soon == true -> SteamApplication.ReleaseState.Unavailable
                else -> SteamApplication.ReleaseState.Released
            },
            controllerSupport = when {
                info.categories?.controller_categoryids?.contains(EStoreCategory.PartialController.ordinal) == true -> SteamApplication.ControllerSupport.Partial
                info.categories?.controller_categoryids?.contains(EStoreCategory.FullController.ordinal) == true -> SteamApplication.ControllerSupport.Full
                else -> SteamApplication.ControllerSupport.None
            },
            reviewData = SteamApplication.ReviewData(
                reviewScore = info.reviews?.summary_filtered?.review_score ?: 0,
                reviewPercentage = info.reviews?.summary_filtered?.percent_positive ?: 0,
                metacriticScore = 0,
                metacriticUrl = "",
            ),
            contentDescriptors = info.content_descriptorids.mapNotNull { SteamApplication.ContentDescriptor.entries.getOrNull(it - 1) },
            tags = info.tags.mapNotNull(StoreItem_Tag::tagid),
            categories = (info.categories?.controller_categoryids.orEmpty() + info.categories?.feature_categoryids.orEmpty() + info.categories?.supported_player_categoryids.orEmpty()).mapNotNull(EStoreCategory.entries::getOrNull),
            assets = SteamApplication.Assets(
                appId = AppId(info.appid!!),
                iconId = info.assets?.community_icon,
                logoId = null,
                clientIconId = info.assets?.community_icon,
                localizedAssets = mapOf(
                    ELanguage.English to SteamApplication.Assets.LocalizedAssetPack(
                        name = info.name.orEmpty(),
                        smallCapsule = info.assets?.small_capsule?.let { str -> EnvironmentConstants.formatSharedStoreAssetUrl(info.appid!!, str) },
                        libraryCapsule = info.assets?.library_capsule?.let { str ->
                            RetinaAsset(
                                path = str,
                                path2x = info.assets?.library_capsule_2x?.let { str2 -> EnvironmentConstants.formatSharedStoreAssetUrl(info.appid!!, str2) }
                            )
                        },
                        libraryHero = info.assets?.library_hero?.let { str ->
                            RetinaAsset(
                                path = str,
                                path2x = info.assets?.library_hero_2x?.let { str2 -> EnvironmentConstants.formatSharedStoreAssetUrl(info.appid!!, str2) }
                            )
                        },
                        libraryLogo = null,
                        libraryHeader = null,
                        libraryHeroBlur = null,
                    )
                )
            ),
            dlcForAppId = null,
            developers = info.basic_info?.developers?.mapNotNull(StoreItem_BasicInfo_CreatorHomeLink::name).orEmpty(),
            franchises = info.basic_info?.franchises?.mapNotNull(StoreItem_BasicInfo_CreatorHomeLink::name).orEmpty(),
            publishers = info.basic_info?.publishers?.mapNotNull(StoreItem_BasicInfo_CreatorHomeLink::name).orEmpty(),
            homePageUrl = "",
            manualUrl = "",
            availableDlc = emptyList(),
            steamDeck = SteamApplication.SteamDeckSupport(
                category = ESteamDeckSupport.entries[info.platforms?.steam_deck_compat_category ?: 0],
                tests = emptyList(),
                testDate = 0L
            )
        )
    }

    /**
     * Converts a database [RoomPicFullAppInfo] into a client-facing [SteamApplication].
     */
    internal fun fromDatabase(
        info: RoomPicsAppInfo,
        liteObject: Boolean = true,
        categories: List<RoomPicsAppInfoCategory> = emptyList(),
        tags: List<RoomStoreTag> = emptyList(),
        descriptors: List<RoomPicsAppInfoContentDescriptor> = emptyList(),
        localizedAssets: List<RoomPicsAppInfoLocalizedAssets> = emptyList(),
        associations: List<RoomPicsAppInfoAssociation> = emptyList(),
    ): SteamApplication {
        return SteamApplication(
            isLiteObject = liteObject,
            id = AppId(info.id),
            type = when (info.type) {
                "game" -> SteamApplication.Type.Game
                "tool" -> SteamApplication.Type.Tool
                "dlc" -> SteamApplication.Type.DLC
                "music" -> SteamApplication.Type.Music
                "video" -> SteamApplication.Type.Video
                "config" -> SteamApplication.Type.Config
                "application" -> SteamApplication.Type.Application
                "beta" -> SteamApplication.Type.Beta
                "demo" -> SteamApplication.Type.Demo
                else -> SteamApplication.Type.Unknown
            },
            name = info.name,
            steamReleaseDate = info.steamReleaseDate,
            originalReleaseDate = 0L,
            supportedOs = emptyList(),
            releaseState = when (info.releaseState.lowercase()) {
                "released" -> SteamApplication.ReleaseState.Released
                "prerelease" -> SteamApplication.ReleaseState.Prerelease
                "preloadonly" -> SteamApplication.ReleaseState.PreloadOnly
                "disabled" -> SteamApplication.ReleaseState.Disabled
                else -> SteamApplication.ReleaseState.Unavailable
            },
            controllerSupport = when (info.controllerSupport.lowercase()) {
                "partial" -> SteamApplication.ControllerSupport.Partial
                "full" -> SteamApplication.ControllerSupport.Full
                else -> SteamApplication.ControllerSupport.None
            },
            reviewData = SteamApplication.ReviewData(
                reviewScore = info.reviewScore,
                reviewPercentage = info.reviewPercentage,
                metacriticScore = info.metacriticScore ?: 0,
                metacriticUrl = info.metacriticUrl.orEmpty(),
            ),
            contentDescriptors = descriptors.mapNotNull { SteamApplication.ContentDescriptor.entries.getOrNull(it.descriptor - 1) },
            tags = tags.map(RoomStoreTag::id),
            categories = categories.mapNotNull { c -> EStoreCategory.entries.getOrNull(c.categoryId) },
            assets = SteamApplication.Assets(
                appId = AppId(info.id),
                iconId = info.iconId,
                logoId = info.logoId,
                clientIconId = info.clientIconId,
                localizedAssets = localizedAssets.filter { roomAssetPack ->
                    ELanguage.Companion.byVdf(roomAssetPack.language) != null
                }.associate { roomAssetPack ->
                    ELanguage.Companion.byVdf(id = roomAssetPack.language)!! to SteamApplication.Assets.LocalizedAssetPack(
                        name = roomAssetPack.name,
                        smallCapsule = roomAssetPack.smallCapsule?.let {
                            EnvironmentConstants.formatSharedStoreAssetUrl(
                                info.id,
                                it
                            )
                        },
                        libraryCapsule = roomAssetPack.libraryCapsule?.toRetina(info.id),
                        libraryHero = roomAssetPack.libraryHero?.toRetina(info.id),
                        libraryHeroBlur = roomAssetPack.libraryHeroBlur?.toRetina(info.id),
                        libraryHeader = roomAssetPack.libraryHeader?.toRetina(info.id),
                        libraryLogo = roomAssetPack.libraryLogo?.toRetina(info.id),
                    )
                }
            ),
            dlcForAppId = info.dlcForAppId?.let(::AppId),
            developers = associations.filter { it.type == "developer" }.map(RoomPicsAppInfoAssociation::name),
            franchises = associations.filter { it.type == "franchise" }.map(RoomPicsAppInfoAssociation::name),
            publishers = associations.filter { it.type == "publisher" }.map(RoomPicsAppInfoAssociation::name),
            homePageUrl = info.homepage.orEmpty(),
            manualUrl = info.manualUrl.orEmpty(),
            availableDlc = emptyList(),
            steamDeck = SteamApplication.SteamDeckSupport(
                category = ESteamDeckSupport.entries[info.steamDeckCompat],
                tests = emptyList(),
                testDate = 0L
            )
        )
    }

    /**
     * Converts a PICS [bruhcollective.itaysonlab.ksteam.models.pics.AppInfo] into a client-facing [SteamApplication].
     */
    internal fun fromPics(pics: AppInfo): SteamApplication {
        val common = pics.common ?: error("[${pics.appId}] PICS common field should not be null!")

        val type = when (common.type.lowercase()) {
            "game" -> SteamApplication.Type.Game
            "tool" -> SteamApplication.Type.Tool
            "dlc" -> SteamApplication.Type.DLC
            "music" -> SteamApplication.Type.Music
            "video" -> SteamApplication.Type.Video
            "config" -> SteamApplication.Type.Config
            "application" -> SteamApplication.Type.Application
            "beta" -> SteamApplication.Type.Beta
            "demo" -> SteamApplication.Type.Demo
            else -> SteamApplication.Type.Unknown
        }

        val releaseState = when (common.releaseState.lowercase()) {
            "released" -> SteamApplication.ReleaseState.Released
            "prerelease" -> SteamApplication.ReleaseState.Prerelease
            "preloadonly" -> SteamApplication.ReleaseState.PreloadOnly
            "disabled" -> SteamApplication.ReleaseState.Disabled
            else -> SteamApplication.ReleaseState.Unavailable
        }

        val controllerSupport = when (common.controllerSupport.lowercase()) {
            "full" -> SteamApplication.ControllerSupport.Full
            "partial" -> SteamApplication.ControllerSupport.Partial
            else -> SteamApplication.ControllerSupport.None
        }

        val contentDescriptors = common.contentDescriptors.mapNotNull { contentDescriptor ->
            SteamApplication.ContentDescriptor.entries.getOrNull((contentDescriptor.toIntOrNull() ?: return@mapNotNull null) - 1)
        }

        val assets = SteamApplication.Assets(
            appId = AppId(pics.appId),
            iconId = common.iconId,
            logoId = common.logoId,
            clientIconId = common.clientIconId,
            localizedAssets = extractLocalizedAssetsFrom(pics, true)
        )

        val steamDeck = SteamApplication.SteamDeckSupport(
            category = common.steamDeckCompat.category.let(ESteamDeckSupport.entries::getOrNull)
                ?: ESteamDeckSupport.Unknown,
            testDate = common.steamDeckCompat.testedOn,
            tests = common.steamDeckCompat.tests.map { test ->
                SteamApplication.SteamDeckSupport.TestResult(
                    display = ESteamDeckTestResult.entries.getOrNull(test.display) ?: ESteamDeckTestResult.Unknown,
                    token = test.token
                )
            }
        )

        return SteamApplication(
            isLiteObject = false,
            id = AppId(pics.appId),
            type = type,
            name = common.name,
            steamReleaseDate = common.steamReleaseDate,
            originalReleaseDate = common.releaseDate,
            supportedOs = common.osList.split(","),
            releaseState = releaseState,
            controllerSupport = controllerSupport,
            reviewData = SteamApplication.ReviewData(
                reviewScore = common.reviewScore,
                reviewPercentage = common.reviewPercentage,
                metacriticScore = common.metacriticScore,
                metacriticUrl = common.metacriticUrl,
            ),
            contentDescriptors = contentDescriptors,
            tags = common.tags,
            categories = common.category.keys.mapNotNull { it.removePrefix("category_").toIntOrNull() }
                .mapNotNull(EStoreCategory.entries::getOrNull),
            assets = assets,
            dlcForAppId = common.dlcForAppId.takeIf { it != 0 }?.let(::AppId),
            developers = common.associations.filter { it.type == "developer" }.map { it.name },
            franchises = common.associations.filter { it.type == "franchise" }.map { it.name },
            publishers = common.associations.filter { it.type == "publisher" }.map { it.name },
            homePageUrl = pics.extended?.homepage.orEmpty(),
            manualUrl = pics.extended?.manualUrl.orEmpty(),
            availableDlc = pics.extended?.listOfDlc?.split(",")?.mapNotNull(String::toIntOrNull) ?: emptyList(),
            steamDeck = steamDeck
        )
    }

    internal fun extractLocalizedAssetsFrom(appInfo: AppInfo, appendCdn: Boolean): Map<ELanguage, SteamApplication.Assets.LocalizedAssetPack> {
        val common = appInfo.common ?: error("[${appInfo.appId}] PICS common field should not be null!")

        return (
                common.nameLocalized.keys +
                        common.smallCapsule.keys +
                        common.libraryFullAssets.libraryCapsule.image.keys +
                        common.libraryFullAssets.libraryLogo.image.keys +
                        common.libraryFullAssets.libraryHeader.image.keys +
                        common.libraryFullAssets.libraryHero.image.keys +
                        common.libraryFullAssets.libraryHeroBlur.image.keys
                ).distinct().mapNotNull { language ->
                ELanguage.Companion.byVdf(language)
            }.associate { language ->
                language to SteamApplication.Assets.LocalizedAssetPack(
                    name = common.nameLocalized[language.vdfName],
                    smallCapsule = common.smallCapsule[language.vdfName],
                    libraryCapsule = common.libraryFullAssets.libraryCapsule.toRetina(
                        appInfo.appId,
                        language.vdfName,
                        appendCdn
                    ),
                    libraryHero = common.libraryFullAssets.libraryHero.toRetina(
                        appInfo.appId,
                        language.vdfName,
                        appendCdn
                    ),
                    libraryHeroBlur = common.libraryFullAssets.libraryHeroBlur.toRetina(
                        appInfo.appId,
                        language.vdfName,
                        appendCdn
                    ),
                    libraryHeader = common.libraryFullAssets.libraryHeader.toRetina(
                        appInfo.appId,
                        language.vdfName,
                        appendCdn
                    ),
                    libraryLogo = common.libraryFullAssets.libraryLogo.toRetina(
                        appInfo.appId,
                        language.vdfName,
                        appendCdn
                    ),
                )
            }
    }

    private fun AppInfo.AppInfoCommon.AppInfoLibraryFullAssets.AppInfoLibraryFullAssetDefinition.toRetina(appId: Int, lang: String, appendCdn: Boolean): SteamApplication.Assets.LocalizedAssetPack.RetinaAsset? {
        return image[lang]?.let { path ->
            if (appendCdn) {
                SteamApplication.Assets.LocalizedAssetPack.RetinaAsset(
                    path = EnvironmentConstants.formatSharedStoreAssetUrl(appId, path),
                    path2x = image2x[lang]?.let { x2 -> EnvironmentConstants.formatSharedStoreAssetUrl(appId, x2) }
                )
            } else {
                SteamApplication.Assets.LocalizedAssetPack.RetinaAsset(
                    path = path,
                    path2x = image2x[lang]
                )
            }
        }
    }

    private fun RoomPicsAppInfoLocalizedAssets.RetinaAsset.toRetina(appId: Int): SteamApplication.Assets.LocalizedAssetPack.RetinaAsset? {
        return SteamApplication.Assets.LocalizedAssetPack.RetinaAsset(
            path = EnvironmentConstants.formatSharedStoreAssetUrl(appId, path),
            path2x = path2x?.let { x2 -> EnvironmentConstants.formatSharedStoreAssetUrl(appId, x2) }
        )
    }
}