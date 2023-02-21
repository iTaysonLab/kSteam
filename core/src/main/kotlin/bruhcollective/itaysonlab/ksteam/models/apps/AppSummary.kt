package bruhcollective.itaysonlab.ksteam.models.apps

import bruhcollective.itaysonlab.ksteam.cdn.StaticAppImageUrl
import bruhcollective.itaysonlab.ksteam.models.AppId
import steam.webui.common.StoreItem

class AppSummary internal constructor(
    val id: AppId,
    val name: String,
) {
    constructor(proto: StoreItem) : this(
        id = AppId(proto.id ?: 0),
        name = proto.name.orEmpty()
    )
}

val AppSummary.header get() = StaticAppImageUrl(id.id to "header.jpg")
val AppSummary.capsuleSmall get() = StaticAppImageUrl(id.id to "capsule_231x87.jpg")
val AppSummary.capsuleLarge get() = StaticAppImageUrl(id.id to "capsule_616x353.jpg")
val AppSummary.pageBackground get() = StaticAppImageUrl(id.id to "page_bg_raw.jpg")
val AppSummary.logoLarge get() = StaticAppImageUrl(id.id to "logo.png")
val AppSummary.libraryEntry get() = StaticAppImageUrl(id.id to "library_600x900.jpg")
val AppSummary.libraryHeader get() = StaticAppImageUrl(id.id to "library_hero.jpg")