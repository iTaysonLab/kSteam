package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants.formatCommunityPublicAssetUrl

data class ProfileItem (
    val imageSmall: String?,
    val imageLarge: String?,
    val name: String,
    val itemTitle: String?,
    val itemDescription: String?,
    val appId: Int,
    val itemType: Int,
    val itemClass: Int,
    val movieWebm: String?,
    val movieMp4: String?,
    val movieWebmSmall: String?,
    val movieMp4Small: String?,
    val flags: Int,
    val itemId: Long
) {
    internal constructor(proto: steam.webui.player.ProfileItem): this(
        imageSmall = proto.image_small.orEmpty().ifNotEmpty(::formatCommunityPublicAssetUrl),
        imageLarge = proto.image_large.orEmpty().ifNotEmpty(::formatCommunityPublicAssetUrl),
        name = proto.name.orEmpty(),
        itemTitle = proto.item_title.orEmpty(),
        itemDescription = proto.item_description.orEmpty(),
        appId = proto.appid ?: 0,
        itemType = proto.item_type ?: 0,
        itemClass = proto.item_class ?: 0,
        movieWebm = proto.movie_webm.orEmpty().ifNotEmpty(::formatCommunityPublicAssetUrl),
        movieMp4 = proto.movie_mp4.orEmpty().ifNotEmpty(::formatCommunityPublicAssetUrl),
        movieWebmSmall = proto.movie_webm_small.orEmpty().ifNotEmpty(::formatCommunityPublicAssetUrl),
        movieMp4Small = proto.movie_mp4_small.orEmpty().ifNotEmpty(::formatCommunityPublicAssetUrl),
        flags = proto.equipped_flags ?: 0,
        itemId = proto.communityitemid ?: 0
    )
}

fun steam.webui.player.ProfileItem?.toAppModel() = this?.let { ProfileItem(it) }

private inline fun <R> String.ifNotEmpty(crossinline defaultValue: (String) -> R): R? = if (isNotEmpty()) defaultValue(this) else null