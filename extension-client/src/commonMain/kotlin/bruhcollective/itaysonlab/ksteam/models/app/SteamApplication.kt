package bruhcollective.itaysonlab.ksteam.models.app

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckTestResult
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory

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
     * "Lite" [SteamApplication] will have these fields empty:
     * - [contentDescriptors]
     * - [tags]
     * - [categories]
     * - [Assets.localizedAssets]
     * - [developers]
     * - [publishers]
     * - [franchises]
     *
     * "Lite" objects are queried from the inbuilt PICS database and are great to use if you only want to display application image/title in UI.
     * They don't require to fetch EVERY associated field, slowing down requests.
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
            icon = iconId?.takeIf { it.isNotEmpty() }?.let { id -> EnvironmentConstants.formatCommunityImageUrl(appId.value, "${id}.jpg") }.orEmpty(),
            logo = logoId?.takeIf { it.isNotEmpty() }?.let { id -> EnvironmentConstants.formatCommunityImageUrl(appId.value, "${id}.jpg") }.orEmpty(),
            clientIcon = clientIconId?.takeIf { it.isNotEmpty() }?.let { id -> EnvironmentConstants.formatCommunityImageUrl(appId.value, "${id}.ico") }.orEmpty(),
            mediumCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "capsule_467x181.jpg"),
            largeCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "capsule_616x353.jpg"),
            heroCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "hero_capsule.jpg"),
            pageBgRaw = EnvironmentConstants.formatSharedStoreAssetUrl(appId.value, "page_bg_raw.jpg"),
            /**
             * internal val AppInfo.capsuleSmall get() = formatStaticAppImageUrl(appId, "capsule_231x87.jpg")
             */
            localizedAssets = localizedAssets
        )

        /**
         * A localized asset pack.
         */
        data class LocalizedAssetPack(
            val name: String?,

            /**
             * A 231x87 capsule image.
             */
            val smallCapsule: String?,

            /**
             * Asset path for library capsule - a 600x900 (w*h) image which is basically a cover art shown in library grid.
             */
            val libraryCapsule: RetinaAsset?,

            /**
             * Asset path for library hero - a key art WITHOUT the logo.
             */
            val libraryHero: RetinaAsset?,

            /**
             * Asset path for library logo. This is an image that appears on top of all layers.
             */
            val libraryLogo: RetinaAsset?,

            /**
             * Asset path for library header - a key art WITH the logo.
             */
            val libraryHeader: RetinaAsset?,

            /**
             * Asset path for blurred library hero - a key art WITHOUT the logo.
             *
             * This might not be available everywhere.
             */
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
}