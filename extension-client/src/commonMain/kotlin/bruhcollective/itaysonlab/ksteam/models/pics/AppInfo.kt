package bruhcollective.itaysonlab.ksteam.models.pics

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants.formatCommunityImageUrl
import bruhcollective.itaysonlab.ksteam.EnvironmentConstants.formatStaticAppImageUrl
import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionAppType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class AppInfo {
    @SerialName("appid")
    var appId: Int = 0

    @SerialName("common")
    var common: AppInfoCommon? = null

    @SerialName("extended")
    var extended: AppInfoExtended? = null

    @SerialName("albummetadata")
    var albumMetadata: AppInfoAlbumMetadata? = null

    // Sub-classes

    @Serializable
    internal class AppInfoCommon {
        @SerialName("name")
        var name: String = ""

        // Strings

        @SerialName("type")
        var type: String = ""

        @SerialName("oslist")
        var osList: String = ""

        @SerialName("releasestate")
        var releaseState: String = ""

        @SerialName("controller_support")
        var controllerSupport: String = ""

        @SerialName("metacritic_fullurl")
        var metacriticUrl: String = ""

        @SerialName("icon")
        var iconId: String = ""

        @SerialName("logo")
        var logoId: String = ""

        @SerialName("logo_small")
        var logoSmallId: String = ""

        @SerialName("clienticon")
        var clientIconId: String = ""

        @SerialName("clienttga")
        var clientTgaId: String = ""

        // Integers

        @SerialName("review_score")
        var reviewScore: Int = 0

        @SerialName("review_percentage")
        var reviewPercentage: Int = 0

        @SerialName("review_score_bombs")
        var reviewScoreBombs: Int = 0

        @SerialName("review_percentage_bombs")
        var reviewPercentageBombs: Int = 0

        @SerialName("mastersubs_granting_app")
        var masterSubPackageId: Int = 0

        @SerialName("original_release_date")
        var releaseDate: Long = 0

        @SerialName("steam_release_date")
        var steamReleaseDate: Long = 0

        @SerialName("metacritic_score")
        var metacriticScore: Int = 0

        @SerialName("primary_genre")
        var primaryGenre: Int = 0

        @SerialName("dlcforappid")
        var dlcForAppId: Int = 0

        // Lists

        @SerialName("content_descriptors")
        var contentDescriptors: List<String> = emptyList()

        @SerialName("genres")
        var genres: List<Int> = emptyList()

        @SerialName("associations")
        var associations: List<AppAssociation> = emptyList()

        @SerialName("store_tags")
        var tags: List<Int> = emptyList()

        @SerialName("eulas")
        var eulas: List<AppEula> = emptyList()

        // Dictionaries

        @SerialName("name_localized")
        var nameLocalized: Map<String, String> = emptyMap()

        @SerialName("small_capsule")
        var smallCapsule: Map<String, String> = emptyMap()

        @SerialName("header_image")
        var headerImages: Map<String, String> = emptyMap()

        @SerialName("category")
        var category: Map<String, Boolean> = emptyMap()

        @SerialName("languages")
        var languages: Map<String, Boolean> = emptyMap()

        @SerialName("supported_languages")
        var supportedLanguages: Map<String, AppSupportedLanguageMatrix?> = emptyMap()

        // Booleans

        @SerialName("has_adult_content")
        var adultContent: Boolean = false

        @SerialName("has_adult_content_violence")
        var adultContentViolence: Boolean = false

        @SerialName("has_adult_content_sex")
        var adultContentSex: Boolean = false

        @SerialName("community_visible_stats")
        var hasStats: Boolean = false

        @SerialName("community_hub_visible")
        var hasContentHub: Boolean = false

        @SerialName("exfgls")
        var excludeFromGameLibrarySharing: Boolean = false

        // Sub-Objects

        @SerialName("steam_deck_compatibility")
        var steamDeckCompat: AppSteamDeckCompatibility = AppSteamDeckCompatibility()

        @SerialName("library_assets_full")
        var libraryFullAssets: AppInfoLibraryFullAssets = AppInfoLibraryFullAssets()

        // Sub-classes

        @Serializable
        class AppInfoLibraryFullAssets {
            @SerialName("library_capsule")
            var libraryCapsule: AppInfoLibraryFullAssetDefinition = AppInfoLibraryFullAssetDefinition()

            @SerialName("library_hero")
            var libraryHero: AppInfoLibraryFullAssetDefinition = AppInfoLibraryFullAssetDefinition()

            @SerialName("library_hero_blur")
            var libraryHeroBlur: AppInfoLibraryFullAssetDefinition = AppInfoLibraryFullAssetDefinition()

            @SerialName("library_logo")
            var libraryLogo: AppInfoLibraryFullAssetDefinition = AppInfoLibraryFullAssetDefinition()

            @SerialName("library_header")
            var libraryHeader: AppInfoLibraryFullAssetDefinition = AppInfoLibraryFullAssetDefinition()

            @Serializable
            class AppInfoLibraryFullAssetDefinition {
                @SerialName("image")
                var image: Map<String, String> = emptyMap()

                @SerialName("image2x")
                var image2x: Map<String, String> = emptyMap()
            }
        }

        @Serializable
        class AppSteamDeckCompatibility {
            @SerialName("category")
            var category: Int = 0

            @SerialName("tests")
            var tests: List<AppSteamDeckCompatTestEntry> = emptyList()

            @SerialName("test_timestamp")
            var testedOn: Long = 0

            // Sub-classes

            @Serializable
            class AppSteamDeckCompatTestEntry {
                @SerialName("display")
                var display: Int = 0

                @SerialName("token")
                var token: String = ""
            }
        }

        @Serializable
        class AppAssociation {
            @SerialName("type")
            var type: String = ""

            @SerialName("name")
            var name: String = ""
        }

        @Serializable
        class AppSupportedLanguageMatrix {
            @SerialName("supported")
            var supported: Boolean = false

            @SerialName("full_audio")
            var fullAudio: Boolean = false

            @SerialName("subtitles")
            var subtitles: Boolean = false
        }

        @Serializable
        class AppEula {
            @SerialName("id")
            var id: String = ""

            @SerialName("name")
            var name: String = ""

            @SerialName("url")
            var url: String = ""

            @SerialName("version")
            var version: String = ""
        }
    }

    @Serializable
    class AppInfoExtended {
        @SerialName("developer")
        var developer: String = ""

        @SerialName("publisher")
        var publisher: String = ""

        @SerialName("homepage")
        var homepage: String = ""

        @SerialName("gamemanualurl")
        var manualUrl: String = ""

        @SerialName("musicalbumforappid")
        var musicAlbumForApp: Int = 0

        @SerialName("listofdlc")
        var listOfDlc: String = ""

        @SerialName("dlcavailableonstore")
        var dlcAvailableOnStore: Boolean = false

        @SerialName("musicalbumavailableonstore")
        var musicAlbumAvailableOnStore: Boolean = false
    }

    @Serializable
    class AppInfoAlbumMetadata {
        @SerialName("tracks")
        var trackList: List<AppInfoMusicTrack> = emptyList()

        @SerialName("metadata")
        var metadata: AppInfoMusicMetadata? = null

        @SerialName("cdn_assets")
        var cdnAssets: AppInfoMusicCdnAssets? = null

        @Serializable
        class AppInfoMusicTrack {
            @SerialName("discnumber")
            var discNumber: Int = 0

            @SerialName("tracknumber")
            var trackNumber: Int = 0

            @SerialName("originalname")
            var originalName: String = ""

            @SerialName("m")
            var minutes: Int = 0

            @SerialName("s")
            var seconds: Int = 0
        }

        @Serializable
        class AppInfoMusicMetadata {
            @SerialName("artist")
            var artist: Map<String, String> = emptyMap()

            @SerialName("composer")
            var composer: Map<String, String> = emptyMap()

            @SerialName("label")
            var label: Map<String, String> = emptyMap()

            @SerialName("othercredits")
            var otherCredits: Map<String, String> = emptyMap()
        }

        @Serializable
        class AppInfoMusicCdnAssets {
            @SerialName("album_cover")
            var albumCoverId: String = ""
        }
    }
}

//

internal val AppInfo.type get() = ECollectionAppType.entries.firstOrNull { it.name.equals(common?.type, ignoreCase = true) } ?: ECollectionAppType.Invalid
internal val AppInfo.icon get() = formatCommunityImageUrl(appId, "${common?.iconId}.jpg")
internal val AppInfo.logo get() = formatCommunityImageUrl(appId, "${common?.logoId}.jpg")
internal val AppInfo.header get() = formatStaticAppImageUrl(appId, "header.jpg")
internal val AppInfo.capsuleSmall get() = formatStaticAppImageUrl(appId, "capsule_231x87.jpg")
internal val AppInfo.capsuleLarge get() = formatStaticAppImageUrl(appId, "capsule_616x353.jpg")
internal val AppInfo.pageBackground get() = formatStaticAppImageUrl(appId, "page_bg_raw.jpg")
internal val AppInfo.logoLarge get() = formatStaticAppImageUrl(appId, "logo.png")
internal val AppInfo.libraryEntry get() = formatStaticAppImageUrl(appId, "library_600x900.jpg")
internal val AppInfo.libraryHeader get() = formatStaticAppImageUrl(appId, "library_hero.jpg")