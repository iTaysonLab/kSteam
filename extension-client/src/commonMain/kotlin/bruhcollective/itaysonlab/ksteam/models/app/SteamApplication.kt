package bruhcollective.itaysonlab.ksteam.models.app

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckTestResult
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import kotlin.collections.component1
import kotlin.collections.component2

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
data class SteamApplication (
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
     *
     * TODO: Int -> Enum
     */
    val categories: List<Int>,

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
    data class SteamDeckSupport (
        val category: ESteamDeckSupport,
        val tests: List<TestResult>,
        val testDate: Long
    ) {
        data class TestResult (
            val display: ESteamDeckTestResult,
            val token: String,
        )
    }

    /**
     * Image assets. kSteam automatically maps languages and replaces relative file names with CDN URLs.
     */
    data class Assets (
        // Icons
        val icon: String,
        val clientIcon: String,
        // English-only assets
        val mediumCapsule: String,
        val largeCapsule: String,
        val heroCapsule: String,
        val logo: String,
        val pageBgRaw: String,
        // Localized assets
        val localizedName: Map<ELanguage, String>,
        val smallCapsule: Map<ELanguage, String>,
        val headerImage: Map<ELanguage, String>,
        val libraryCapsule: Map<ELanguage, String>,
        val libraryHero: Map<ELanguage, String>,
        val libraryLogo: Map<ELanguage, String>,
    )

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
    data class ReviewData (
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
         * Converts a PICS [AppInfo] into a client-facing [SteamApplication].
         */
        internal fun fromPics(pics: AppInfo): SteamApplication {
            val common = pics.common ?: error("[${pics.appId}] PICS common field should not be null!")

            val type = when (common.type.lowercase()) {
                "game" -> Type.Game
                "tool" -> Type.Tool
                "dlc" -> Type.DLC
                "muAsic" -> Type.Music
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
                icon = EnvironmentConstants.formatCommunityImageUrl(pics.appId, "${common.iconId}.jpg"),
                logo = EnvironmentConstants.formatCommunityImageUrl(pics.appId, "${common.logoId}.jpg"),
                clientIcon = EnvironmentConstants.formatCommunityImageUrl(pics.appId, "${common.clientIconId}.ico"),
                //
                localizedName = common.nameLocalized.entries.filter { (language, _) ->
                    ELanguage.byVdf(language) != null
                }.associate { (language, name) ->
                    ELanguage.byVdf(language)!! to name
                },
                //
                smallCapsule = common.smallCapsule.mapVdfLocalizedToCdnUrl(pics.appId),
                headerImage = common.headerImages.mapVdfLocalizedToCdnUrl(pics.appId),
                //
                mediumCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(pics.appId, "capsule_467x181.jpg"),
                largeCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(pics.appId, "capsule_616x353.jpg"),
                heroCapsule = EnvironmentConstants.formatSharedStoreAssetUrl(pics.appId, "hero_capsule.jpg"),
                pageBgRaw = EnvironmentConstants.formatSharedStoreAssetUrl(pics.appId, "page_bg_raw.jpg"),
                //
                libraryHero = common.libraryFullAssets?.libraryHero?.image?.mapVdfLocalizedToCdnUrl(pics.appId) ?: emptyMap(),
                libraryLogo = common.libraryFullAssets?.libraryLogo?.image?.mapVdfLocalizedToCdnUrl(pics.appId) ?: emptyMap(),
                libraryCapsule = common.libraryFullAssets?.libraryCapsule?.image?.mapVdfLocalizedToCdnUrl(pics.appId) ?: emptyMap(),
            )

            val steamDeck = SteamDeckSupport(
                category = common.steamDeckCompat?.category?.let { ESteamDeckSupport.entries.getOrNull(it) } ?: ESteamDeckSupport.Unknown,
                testDate = common.steamDeckCompat?.testedOn ?: 0,
                tests = common.steamDeckCompat?.tests?.map { test ->
                    SteamDeckSupport.TestResult(
                        display = ESteamDeckTestResult.entries.getOrNull(test.display) ?: ESteamDeckTestResult.Unknown,
                        token = test.token
                    )
                } ?: emptyList()
            )

            return SteamApplication(
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
                categories = common.category.keys.mapNotNull { it.removePrefix("category_").toIntOrNull() },
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

        private fun Map<String, String>.mapVdfLocalizedToCdnUrl(appId: Int) = entries.filter { (language, _) ->
            ELanguage.byVdf(language) != null
        }.associate { (language, fileName) ->
            ELanguage.byVdf(language)!! to EnvironmentConstants.formatSharedStoreAssetUrl(appId, fileName)
        }
    }
}