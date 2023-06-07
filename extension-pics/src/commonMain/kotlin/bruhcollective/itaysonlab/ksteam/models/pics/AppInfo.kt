package bruhcollective.itaysonlab.ksteam.models.pics

import bruhcollective.itaysonlab.ksteam.cdn.CommunityAppImageUrl
import bruhcollective.itaysonlab.ksteam.cdn.StaticAppImageUrl
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

val AppInfo.icon get() = CommunityAppImageUrl(appId to common.iconId)
val AppInfo.logo get() = CommunityAppImageUrl(appId to common.logoId)
val AppInfo.header get() = StaticAppImageUrl(appId to "header.jpg")
val AppInfo.capsuleSmall get() = StaticAppImageUrl(appId to "capsule_231x87.jpg")
val AppInfo.capsuleLarge get() = StaticAppImageUrl(appId to "capsule_616x353.jpg")
val AppInfo.pageBackground get() = StaticAppImageUrl(appId to "page_bg_raw.jpg")
val AppInfo.logoLarge get() = StaticAppImageUrl(appId to "logo.png")
val AppInfo.libraryEntry get() = StaticAppImageUrl(appId to "library_600x900.jpg")
val AppInfo.libraryHeader get() = StaticAppImageUrl(appId to "library_hero.jpg")