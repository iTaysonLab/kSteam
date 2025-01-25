package bruhcollective.itaysonlab.ksteam.models.app

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.RoomPicsAppInfoLocalizedAssets
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.RoomPicsFullAppInfo
import bruhcollective.itaysonlab.ksteam.database.room.entity.store.RoomStoreTag
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.Assets.LocalizedAssetPack
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckTestResult
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo

/**
 * Defines an application available on Steam. Returns most of the foreground information, except for descriptions/screenshots/reviews.
 *
 * This is a "layer" between PICS, Store and the end application. kSteam will try to provide as much data from offline source, but in some cases it can fall back to calling API methods.
 *
 * Because of Steam's API nature, this class will be partially complete based on:
 * - user's availability in library: if the game is purchased, much more information will be available
 * - calling handler: data requests from [Store] handler will contain both data from PICS and Store, while [Library]/[PICS] handlers will return only local data
 *
 * Steam not only hosts games, but also applications, DLC's, music albums and videos. kSteam tries to support most of special properties, however:
 * - music/video information is currently available only for purchased content via PICS results
 * - depot information is not yet available due to VDF parser incompatibility
 */
data class SteamApplication(
    /**
     * This will return true is the object is in "lite" state.
     *
     * "Lite" [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication] will have only following info:
     * - [id]
     * - [type]
     * - [name]
     * - [steamReleaseDate]
     * - [releaseState]
     * - [controllerSupport]
     * - [ReviewData.reviewScore]
     * - [ReviewData.metacriticScore]
     * - [tags]
     * - [Assets.clientIcon]
     * - [Assets.localizedAssets]
     * - [categories]
     * - [SteamDeckSupport.category]
     *
     * "Lite" objects are queried from the inbuilt PICS database and are great to use if you only want to display application image/title in UI.
     */
    val isLiteObject: Boolean,

    /**
     * Steam application ID.
     */
    val id: AppId,

    /**
     * Type of the application.
     */
    val type: Type,

    /**
     * Application name.
     */
    val name: String,

    /**
     * Release date on Steam.
     */
    val steamReleaseDate: Long,

    /**
     * Original release date. This will be non-zero in case the game was released **on PC** before Steam.
     */
    val originalReleaseDate: Long,

    /**
     * Supported operating systems.
     */
    val supportedOs: List<String>,

    /**
     * Release state.
     */
    val releaseState: ReleaseState,

    /**
     * Controller support.
     */
    val controllerSupport: ControllerSupport,

    /**
     * Review information.
     */
    val reviewData: ReviewData,

    /**
     * List of content descriptors attached to an application.
     */
    val contentDescriptors: List<ContentDescriptor>,

    /**
     * List of tag IDs attached to an application.
     */
    val tags: List<Int>,

    /**
     * Assets related to this application.
     */
    val assets: Assets,

    /**
     * If this application is a DLC, this will be the main app ID.
     */
    val dlcForAppId: AppId?,

    /**
     * Categories (Steam Achievements, VAC, Trading Cards).
     */
    val categories: List<EStoreCategory>,

    /**
     * Steam Deck support information.
     */
    val steamDeck: SteamDeckSupport,

    /**
     * List of developers.
     */
    val developers: List<String>,

    /**
     * List of publishers.
     */
    val publishers: List<String>,

    /**
     * List of franchises.
     */
    val franchises: List<String>,

    /**
     * URL of game homepage.
     */
    val homePageUrl: String,

    /**
     * Manual URL.
     */
    val manualUrl: String,

    /**
     * If this is an application, this will be a list of DLC application ID that were released
     */
    val availableDlc: List<Int>
) {
    /**
     * Steam Deck support information.
     */
    data class SteamDeckSupport(
        val category: ESteamDeckSupport,
        val tests: List<TestResult>,
        val testDate: Long
    ) {
        data class TestResult(
            val display: ESteamDeckTestResult,
            val token: String,
        )
    }

    /**
     * Image assets. kSteam automatically maps languages and replaces relative file names with CDN URLs.
     */
    data class Assets(
        /**
         * Path to 256x256 icon in ICO format.
         */
        val icon: String,

        /**
         * Path to 32x32 icon in JPG format.
         */
        val clientIcon: String,

        // English-only assets
        val mediumCapsule: String,
        val largeCapsule: String,
        val heroCapsule: String,
        val logo: String,
        val pageBgRaw: String,

        /**
         * Localized asset packs.
         *
         * At least a pack for [ELanguage.English] will be available.
         */
        val localizedAssets: Map<ELanguage, LocalizedAssetPack>,
    ) {
        constructor(
            appId: AppId,
            iconId: String?,
            logoId: String?,
            clientIconId: String?,
            localizedAssets: Map<ELanguage, LocalizedAssetPack>
        ): this(
            icon = iconId?.let { id -> EnvironmentConstants.formatCommunityImageUrl(appId.value, "${id}.jpg") }.orEmpty(),
            logo = logoId?.let { id -> EnvironmentConstants.formatCommunityImageUrl(appId.value, "${id}.jpg") }.orEmpty(),
            clientIcon = clientIconId?.let { id -> EnvironmentConstants.formatCommunityImageUrl(appId.value, "${id}.icon") }.orEmpty(),
            mediumCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "capsule_467x181.jpg"),
            largeCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "capsule_616x353.jpg"),
            heroCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "hero_capsule.jpg"),
            pageBgRaw = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "page_bg_raw.jpg"),
            localizedAssets = localizedAssets
        )

        /**
         * A localized asset pack.
         */
        data class LocalizedAssetPack(
            val name: String?,
            val smallCapsule: String?,
            val headerImage: String?,
            val libraryCapsule: RetinaAsset?,
            val libraryHero: RetinaAsset?,
            val libraryLogo: RetinaAsset?,
            val libraryHeader: RetinaAsset?,
            val libraryHeroBlur: RetinaAsset?
        ) {
            /**
             * Retina-capable asset.
             */
            data class RetinaAsset(
                /**
                 * Path to standard-sized image.
                 */
                val path: String,

                /**
                 * Path to image, but 2 times larger. Can be null.
                 */
                val path2x: String? = null
            )
        }
    }

    enum class ContentDescriptor {
        NudityOrSexualContent,
        FrequentViolenceOrGore,
        AdultOnlySexualContent,
        GratuitousSexualContent,
        AnyMatureContent
    }

    /**
     * Application review information.
     */
    data class ReviewData(
        /**
         * Steam review score category (Mostly Positive, Mostly Negative...).
         *
         * TODO: Int -> Enum
         */
        val reviewScore: Int,

        /**
         * Steam review score percentage (90% of reviews are positive...)
         */
        val reviewPercentage: Int,

        /**
         * Metacritic score (0-100).
         */
        val metacriticScore: Int,

        /**
         * Metacritic page URL
         */
        val metacriticUrl: String
    )

    enum class ReleaseState {
        /**
         * App is not available on the Steam Store (however, it is available for download if the account owns it)
         */
        Unavailable,

        /**
         * App is not released on the Steam Store (pre-purchase). It also means playtesting, but the game page itself can be accessed publicly.
         */
        Prerelease,

        /**
         * App is not released on the Steam Store, but can be pre-downloaded. This mostly applies to "download before release" games, because the content is still encrypted.
         */
        PreloadOnly,

        /**
         * App is fully released on the Steam Store and available for purchasing/playing.
         */
        Released,

        /**
         * Disabled - is there any apps that has this flag?
         */
        Disabled
    }

    enum class ControllerSupport {
        None,
        Partial,
        Full
    }

    enum class Type {
        /**
         * Game. Most popular content and is mostly supported by kSteam.
         */
        Game,

        /**
         * Tool. Mostly dedicated servers, Linux compatibility layers.
         */
        Tool,

        /**
         * DLC. An addition to existing [Game].
         */
        DLC,

        /**
         * Music. An addition to existing [Game], but unlike [DLC], contains music tracks (and does not require purchasing of linked [Game]).
         *
         * Special metadata is available only for purchased content.
         */
        Music,

        /**
         * Video. An addition to existing [Game], but unlike [DLC], contains a video file (and does not require purchasing of linked [Game]).
         *
         * Special metadata is available only for purchased content.
         */
        Video,

        /**
         * Configuration. Special rule sets that are not available for public view on the Steam store.
         */
        Config,

        /**
         * Demo. Linked to [Game], provides a specially crafted demo version of a game or application.
         */
        Demo,

        /**
         * Application. Ordinary applications published on Steam.
         */
        Application,

        /**
         * Beta. Linked to not released [Game], this serves as an early-access link.
         */
        Beta,

        /**
         * Unknown. Be careful - this might be an unknown object, or just an error when parsing strings.
         */
        Unknown
    }

    //

    companion object {
        /**
         * Converts a database [RoomPicFullAppInfo] into a client-facing [SteamApplication].
         */
        internal fun fromDatabase(info: RoomPicsFullAppInfo): SteamApplication {
            return SteamApplication(
                isLiteObject = true,
                id = AppId(info.appInfo.id),
                type = when (info.appInfo.type.lowercase()) {
                    "game" -> Type.Game
                    "tool" -> Type.Tool
                    "dlc" -> Type.DLC
                    "music" -> Type.Music
                    "video" -> Type.Video
                    "config" -> Type.Config
                    "application" -> Type.Application
                    "beta" -> Type.Beta
                    "demo" -> Type.Demo
                    else -> Type.Unknown
                },
                name = info.appInfo.name,
                steamReleaseDate = info.appInfo.steamReleaseDate,
                originalReleaseDate = 0L,
                supportedOs = emptyList(),
                releaseState = when (info.appInfo.releaseState.lowercase()) {
                    "released" -> ReleaseState.Released
                    "prerelease" -> ReleaseState.Prerelease
                    "preloadonly" -> ReleaseState.PreloadOnly
                    "disabled" -> ReleaseState.Disabled
                    else -> ReleaseState.Unavailable
                },
                controllerSupport = when (info.appInfo.controllerSupport.lowercase()) {
                    "partial" -> ControllerSupport.Partial
                    "full" -> ControllerSupport.Full
                    else -> ControllerSupport.None
                },
                reviewData = ReviewData(
                    reviewScore = info.appInfo.reviewScore,
                    reviewPercentage = 0,
                    metacriticScore = info.appInfo.metacriticScore ?: 0,
                    metacriticUrl = "",
                ),
                contentDescriptors = emptyList(),
                tags = info.tags.map(RoomStoreTag::id),
                categories = info.categories.mapNotNull { c -> EStoreCategory.entries.getOrNull(c.categoryId) },
                assets = Assets(
                    appId = AppId(info.appInfo.id),
                    iconId = info.appInfo.iconId,
                    logoId = info.appInfo.logoId,
                    clientIconId = info.appInfo.clientIconId,
                    localizedAssets = info.localizedAssets.associate { roomAssetPack ->
                        ELanguage.byVdf(id = roomAssetPack.language)!! to LocalizedAssetPack(
                            name = roomAssetPack.name,
                            smallCapsule = roomAssetPack.smallCapsule,
                            headerImage = roomAssetPack.headerImage,
                            libraryCapsule = roomAssetPack.libraryCapsule?.toRetina(),
                            libraryHero = roomAssetPack.libraryHero?.toRetina(),
                            libraryHeroBlur = roomAssetPack.libraryHeroBlur?.toRetina(),
                            libraryHeader = roomAssetPack.libraryHeader?.toRetina(),
                            libraryLogo = roomAssetPack.libraryLogo?.toRetina(),
                        )
                    }
                ),
                dlcForAppId = info.appInfo.dlcForAppId?.let(::AppId),
                developers = emptyList(),
                franchises = emptyList(),
                publishers = emptyList(),
                homePageUrl = "",
                manualUrl = "",
                availableDlc = emptyList(),
                steamDeck = SteamDeckSupport(
                    category = ESteamDeckSupport.entries[info.appInfo.steamDeckCompat],
                    tests = emptyList(),
                    testDate = 0L
                )
            )
        }

        /**
         * Converts a PICS [AppInfo] into a client-facing [SteamApplication].
         */
        internal fun fromPics(pics: AppInfo): SteamApplication {
            val common = pics.common ?: error("[${pics.appId}] PICS common field should not be null!")

            val type = when (common.type.lowercase()) {
                "game" -> Type.Game
                "tool" -> Type.Tool
                "dlc" -> Type.DLC
                "music" -> Type.Music
                "video" -> Type.Video
                "config" -> Type.Config
                "application" -> Type.Application
                "beta" -> Type.Beta
                "demo" -> Type.Demo
                else -> Type.Unknown
            }

            val releaseState = when (common.releaseState.lowercase()) {
                "released" -> ReleaseState.Released
                "prerelease" -> ReleaseState.Prerelease
                "preloadonly" -> ReleaseState.PreloadOnly
                "disabled" -> ReleaseState.Disabled
                else -> ReleaseState.Unavailable
            }

            val controllerSupport = when (common.controllerSupport.lowercase()) {
                "full" -> ControllerSupport.Full
                "partial" -> ControllerSupport.Partial
                else -> ControllerSupport.None
            }

            val contentDescriptors = common.contentDescriptors.mapNotNull { contentDescriptor ->
                ContentDescriptor.entries.getOrNull(contentDescriptor.toIntOrNull() ?: return@mapNotNull null)
            }

            val assets = Assets(
                appId = AppId(pics.appId),
                iconId = common.iconId,
                logoId = common.logoId,
                clientIconId = common.clientIconId,
                localizedAssets = extractLocalizedAssetsFrom(pics)
            )

            val steamDeck = SteamDeckSupport(
                category = common.steamDeckCompat.category.let(ESteamDeckSupport.entries::getOrNull)
                    ?: ESteamDeckSupport.Unknown,
                testDate = common.steamDeckCompat.testedOn,
                tests = common.steamDeckCompat.tests.map { test ->
                    SteamDeckSupport.TestResult(
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
                reviewData = ReviewData(
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

        internal fun extractLocalizedAssetsFrom(appInfo: AppInfo): Map<ELanguage, LocalizedAssetPack> {
            val common = appInfo.common ?: error("[${appInfo.appId}] PICS common field should not be null!")

            return (
                    common.nameLocalized.keys +
                            common.smallCapsule.keys +
                            common.headerImages.keys +
                            common.libraryFullAssets.libraryCapsule.image.keys +
                            common.libraryFullAssets.libraryLogo.image.keys +
                            common.libraryFullAssets.libraryHeader.image.keys +
                            common.libraryFullAssets.libraryHero.image.keys +
                            common.libraryFullAssets.libraryHeroBlur.image.keys
                    ).distinct().associate { language ->
                    ELanguage.byVdf(language)!! to LocalizedAssetPack(
                        name = common.nameLocalized[language],
                        smallCapsule = common.smallCapsule[language],
                        headerImage = common.headerImages[language],
                        libraryCapsule = common.libraryFullAssets.libraryCapsule.toRetina(language),
                        libraryHero = common.libraryFullAssets.libraryHero.toRetina(language),
                        libraryHeroBlur = common.libraryFullAssets.libraryHeroBlur.toRetina(language),
                        libraryHeader = common.libraryFullAssets.libraryHeader.toRetina(language),
                        libraryLogo = common.libraryFullAssets.libraryLogo.toRetina(language),
                    )
                }
        }

        private fun AppInfo.AppInfoCommon.AppInfoLibraryFullAssets.AppInfoLibraryFullAssetDefinition.toRetina(lang: String): LocalizedAssetPack.RetinaAsset? {
            return image[lang]?.let { path ->
                LocalizedAssetPack.RetinaAsset(
                    path = path,
                    path2x = image2x[lang]
                )
            }
        }

        private fun RoomPicsAppInfoLocalizedAssets.RetinaAsset.toRetina(): LocalizedAssetPack.RetinaAsset? {
            return LocalizedAssetPack.RetinaAsset(path = path, path2x = path2x)
        }
    }
}