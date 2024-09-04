@file:UseSerializers(
    MutableRealmIntKSerializer::class,
    RealmAnyKSerializer::class,
    RealmDictionaryKSerializer::class,
    RealmInstantKSerializer::class,
    RealmListKSerializer::class,
    RealmSetKSerializer::class,
    RealmUUIDKSerializer::class
)

package bruhcollective.itaysonlab.ksteam.models.pics

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants.formatCommunityImageUrl
import bruhcollective.itaysonlab.ksteam.EnvironmentConstants.formatStaticAppImageUrl
import bruhcollective.itaysonlab.ksteam.models.enums.EAppType
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.serializers.*
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmDictionary
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
internal class AppInfo: RealmObject {
    // Fixes https://github.com/realm/realm-kotlin/issues/1567
    companion object

    @PrimaryKey
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
    internal class AppInfoCommon: EmbeddedRealmObject {
        // Fixes https://github.com/realm/realm-kotlin/issues/1567
        companion object

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
        var contentDescriptors: RealmList<String> = realmListOf()

        @SerialName("genres")
        var genres: RealmList<Int> = realmListOf()

        @SerialName("associations")
        var associations: RealmList<AppAssociation> = realmListOf()

        @SerialName("store_tags")
        var tags: RealmList<Int> = realmListOf()

        @SerialName("eulas")
        var eulas: RealmList<AppEula> = realmListOf()

        // Dictionaries

        @SerialName("name_localized")
        var nameLocalized: RealmDictionary<String> = realmDictionaryOf()

        @SerialName("small_capsule")
        var smallCapsule: RealmDictionary<String> = realmDictionaryOf()

        @SerialName("header_image")
        var headerImages: RealmDictionary<String> = realmDictionaryOf()

        @SerialName("category")
        var category: RealmDictionary<Boolean> = realmDictionaryOf()

        @SerialName("languages")
        var languages: RealmDictionary<Boolean> = realmDictionaryOf()

        @SerialName("supported_languages")
        var supportedLanguages: RealmDictionary<AppSupportedLanguageMatrix?> = realmDictionaryOf()

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
        var steamDeckCompat: AppSteamDeckCompatibility? = null

        @SerialName("library_assets_full")
        var libraryFullAssets: AppInfoLibraryFullAssets? = null

        // Sub-classes

        @Serializable
        class AppInfoLibraryFullAssets: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

            @SerialName("library_capsule")
            var libraryCapsule: AppInfoLibraryFullAssetDefinition? = null

            @SerialName("library_hero")
            var libraryHero: AppInfoLibraryFullAssetDefinition? = null

            @SerialName("library_logo")
            var libraryLogo: AppInfoLibraryFullAssetDefinition? = null

            @Serializable
            class AppInfoLibraryFullAssetDefinition: EmbeddedRealmObject {
                // Fixes https://github.com/realm/realm-kotlin/issues/1567
                companion object

                @SerialName("image")
                var image: RealmDictionary<String> = realmDictionaryOf()

                @SerialName("image2x")
                var image2x: RealmDictionary<String> = realmDictionaryOf()
            }
        }

        @Serializable
        class AppSteamDeckCompatibility: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

            @SerialName("category")
            var category: Int = 0

            @SerialName("tests")
            var tests: RealmList<AppSteamDeckCompatTestEntry> = realmListOf()

            @SerialName("test_timestamp")
            var testedOn: Long = 0

            // Sub-classes

            @Serializable
            class AppSteamDeckCompatTestEntry: EmbeddedRealmObject {
                // Fixes https://github.com/realm/realm-kotlin/issues/1567
                companion object

                @SerialName("display")
                var display: Int = 0

                @SerialName("token")
                var token: String = ""
            }
        }

        @Serializable
        class AppAssociation: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

            @SerialName("type")
            var type: String = ""

            @SerialName("name")
            var name: String = ""
        }

        @Serializable
        class AppSupportedLanguageMatrix: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

            @SerialName("supported")
            var supported: Boolean = false

            @SerialName("full_audio")
            var fullAudio: Boolean = false

            @SerialName("subtitles")
            var subtitles: Boolean = false
        }

        @Serializable
        class AppEula: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

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
    class AppInfoExtended: EmbeddedRealmObject {
        // Fixes https://github.com/realm/realm-kotlin/issues/1567
        companion object

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
    class AppInfoAlbumMetadata: EmbeddedRealmObject {
        // Fixes https://github.com/realm/realm-kotlin/issues/1567
        companion object

        @SerialName("tracks")
        var trackList: RealmList<AppInfoMusicTrack> = realmListOf()

        @SerialName("metadata")
        var metadata: AppInfoMusicMetadata? = null

        @SerialName("cdn_assets")
        var cdnAssets: AppInfoMusicCdnAssets? = null

        @Serializable
        class AppInfoMusicTrack: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

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
        class AppInfoMusicMetadata: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

            @SerialName("artist")
            var artist: RealmDictionary<String> = realmDictionaryOf()

            @SerialName("composer")
            var composer: RealmDictionary<String> = realmDictionaryOf()

            @SerialName("label")
            var label: RealmDictionary<String> = realmDictionaryOf()

            @SerialName("othercredits")
            var otherCredits: RealmDictionary<String> = realmDictionaryOf()
        }

        @Serializable
        class AppInfoMusicCdnAssets: EmbeddedRealmObject {
            // Fixes https://github.com/realm/realm-kotlin/issues/1567
            companion object

            @SerialName("album_cover")
            var albumCoverId: String = ""
        }
    }
}

//

internal val AppInfo.type get() = EAppType.entries.firstOrNull { it.name.equals(common?.type, ignoreCase = true) } ?: EAppType.Invalid
internal val AppInfo.icon get() = formatCommunityImageUrl(appId, "${common?.iconId}.jpg")
internal val AppInfo.logo get() = formatCommunityImageUrl(appId, "${common?.logoId}.jpg")
internal val AppInfo.header get() = formatStaticAppImageUrl(appId, "header.jpg")
internal val AppInfo.capsuleSmall get() = formatStaticAppImageUrl(appId, "capsule_231x87.jpg")
internal val AppInfo.capsuleLarge get() = formatStaticAppImageUrl(appId, "capsule_616x353.jpg")
internal val AppInfo.pageBackground get() = formatStaticAppImageUrl(appId, "page_bg_raw.jpg")
internal val AppInfo.logoLarge get() = formatStaticAppImageUrl(appId, "logo.png")
internal val AppInfo.libraryEntry get() = formatStaticAppImageUrl(appId, "library_600x900.jpg")
internal val AppInfo.libraryHeader get() = formatStaticAppImageUrl(appId, "library_hero.jpg")