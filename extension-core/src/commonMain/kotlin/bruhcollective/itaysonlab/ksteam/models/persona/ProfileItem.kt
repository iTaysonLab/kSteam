package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.cdn.CdnUrl
import bruhcollective.itaysonlab.ksteam.cdn.CommunityPublicAssetImageUrl
import bruhcollective.itaysonlab.ksteam.platform.Immutable

@Immutable
data class ProfileItem internal constructor(
    val imageSmall: CdnUrl?,
    val imageLarge: CdnUrl?,
    val name: String,
    val itemTitle: String?,
    val itemDescription: String?,
    val appId: Int,
    val itemType: Int,
    val itemClass: Int,
    val movieWebm: CdnUrl?,
    val movieMp4: CdnUrl?,
    val movieWebmSmall: CdnUrl?,
    val movieMp4Small: CdnUrl?,
    val flags: Int,
    val itemId: Long
) {
    internal constructor(proto: steam.webui.player.ProfileItem): this(
        imageSmall = proto.image_small.orEmpty().ifNotEmpty(::CommunityPublicAssetImageUrl),
        imageLarge = proto.image_large.orEmpty().ifNotEmpty(::CommunityPublicAssetImageUrl),
        name = proto.name.orEmpty(),
        itemTitle = proto.item_title.orEmpty(),
        itemDescription = proto.item_description.orEmpty(),
        appId = proto.appid ?: 0,
        itemType = proto.item_type ?: 0,
        itemClass = proto.item_class ?: 0,
        movieWebm = proto.movie_webm.orEmpty().ifNotEmpty(::CommunityPublicAssetImageUrl),
        movieMp4 = proto.movie_mp4.orEmpty().ifNotEmpty(::CommunityPublicAssetImageUrl),
        movieWebmSmall = proto.movie_webm_small.orEmpty().ifNotEmpty(::CommunityPublicAssetImageUrl),
        movieMp4Small = proto.movie_mp4_small.orEmpty().ifNotEmpty(::CommunityPublicAssetImageUrl),
        flags = proto.equipped_flags ?: 0,
        itemId = proto.communityitemid ?: 0
    )
}

fun steam.webui.player.ProfileItem?.toAppModel() = this?.let { ProfileItem(it) }

private inline fun <R> String.ifNotEmpty(crossinline defaultValue: (String) -> R): R? = if (isNotEmpty()) defaultValue(this) else null