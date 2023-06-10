package bruhcollective.itaysonlab.ksteam.models.apps

import bruhcollective.itaysonlab.ksteam.cdn.SteamCdn.formatStaticAppImageUrl
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import steam.webui.common.StoreItem

@Immutable
class AppSummary(
    val id: Int,
    val name: String,
) {
    constructor(proto: StoreItem) : this(
        id = proto.id ?: 0,
        name = proto.name.orEmpty()
    )
}

val AppSummary.header get() = formatStaticAppImageUrl(id, "header.jpg")
val AppSummary.capsuleSmall get() = formatStaticAppImageUrl(id, "capsule_231x87.jpg")
val AppSummary.capsuleLarge get() = formatStaticAppImageUrl(id, "capsule_616x353.jpg")
val AppSummary.pageBackground get() = formatStaticAppImageUrl(id, "page_bg_raw.jpg")
val AppSummary.logoLarge get() = formatStaticAppImageUrl(id, "logo.png")
val AppSummary.libraryEntry get() = formatStaticAppImageUrl(id, "library_600x900.jpg")
val AppSummary.libraryHeader get() = formatStaticAppImageUrl(id, "library_hero.jpg")