package bruhcollective.itaysonlab.ksteam.models.pics

import bruhcollective.itaysonlab.ksteam.cdn.SteamCdn.formatCommunityImageUrl
import bruhcollective.itaysonlab.ksteam.cdn.SteamCdn.formatStaticAppImageUrl
import bruhcollective.itaysonlab.ksteam.models.enums.EAppType
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class AppInfo internal constructor(
    @SerialName("appid") val appId: Int,
    val common: AppInfoCommon = AppInfoCommon()
) {
    @Serializable
    @Immutable
    data class AppInfoCommon internal constructor(
        val name: String = "",
        val type: String = "",
        @SerialName("oslist") val osList: String = "",
        @SerialName("content_descriptors") val contentDescriptors: List<String> = emptyList(),
        @SerialName("name_localized") val nameLocalized: Map<String, String> = emptyMap(),
        @SerialName("releasestate") val releaseState: String = "",
        @SerialName("has_adult_content") val adultContent: Boolean = false,
        @SerialName("has_adult_content_violence") val adultContentViolence: Boolean = false,
        @SerialName("steam_deck_compatibility") val steamDeckCompat: SteamDeckCompatibility = SteamDeckCompatibility(),
        @SerialName("controller_support") val controllerSupport: String = "",
        @SerialName("small_capsule") val smallCapsule: Map<String, String> = emptyMap(),
        @SerialName("header_image") val headerImages: Map<String, String> = emptyMap(),
        val genres: List<Int> = emptyList(),
        val associations: List<AppInfoAssociation> = emptyList(),
        val category: Map<String, Boolean> = emptyMap(),
        @SerialName("community_visible_stats") val hasStats: Boolean = false,
        @SerialName("community_hub_visible") val hasContentHub: Boolean = false,
        @SerialName("store_tags") val tags: List<Int> = emptyList(),
        @SerialName("review_score") val reviewScore: Int = 0,
        @SerialName("review_percentage") val reviewPercentage: Int = 0,
        @SerialName("mastersubs_granting_app") val masterSubPackageId: Int = 0,
        @SerialName("original_release_date") val releaseDate: Long = 0,
        @SerialName("steam_release_date") val steamReleaseDate: Long = 0,
        @SerialName("metacritic_score") val metacriticScore: Int = 0,
        @SerialName("metacritic_fullurl") val metacriticUrl: String = "",
        @SerialName("icon") val iconId: String = "",
        @SerialName("logo") val logoId: String = "",
    ) {
        @Serializable
        @Immutable
        data class SteamDeckCompatibility internal constructor(
            val category: Int = 0,
            val tests: List<SteamDeckCompatTestEntry> = emptyList()
        ) {
            @Serializable
            @Immutable
            data class SteamDeckCompatTestEntry internal constructor(
                val display: Int = 0,
                val token: String = ""
            )
        }

        @Serializable
        @Immutable
        data class AppInfoAssociation internal constructor(
            val type: String = "",
            val name: String = ""
        )
    }
}

//

val AppInfo.type get() = EAppType.values().firstOrNull { it.name.equals(common.type, ignoreCase = true) } ?: EAppType.Invalid

val AppInfo.icon get() = formatCommunityImageUrl(appId, "${common.iconId}.jpg")
val AppInfo.logo get() = formatCommunityImageUrl(appId, "${common.logoId}.jpg")
val AppInfo.header get() = formatStaticAppImageUrl(appId, "header.jpg")
val AppInfo.capsuleSmall get() = formatStaticAppImageUrl(appId, "capsule_231x87.jpg")
val AppInfo.capsuleLarge get() = formatStaticAppImageUrl(appId, "capsule_616x353.jpg")
val AppInfo.pageBackground get() = formatStaticAppImageUrl(appId, "page_bg_raw.jpg")
val AppInfo.logoLarge get() = formatStaticAppImageUrl(appId, "logo.png")
val AppInfo.libraryEntry get() = formatStaticAppImageUrl(appId, "library_600x900.jpg")
val AppInfo.libraryHeader get() = formatStaticAppImageUrl(appId, "library_hero.jpg")