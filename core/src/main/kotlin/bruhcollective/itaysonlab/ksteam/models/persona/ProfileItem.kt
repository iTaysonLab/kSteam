package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.cdn.CommunityPublicAssetImageUrl

data class ProfileItem internal constructor(
    val imageSmall: CommunityPublicAssetImageUrl,
    val imageLarge: CommunityPublicAssetImageUrl,
    val name: String,
    val itemTitle: String?,
    val itemDescription: String?,
    val appId: Int,
    val itemType: Int,
    val itemClass: Int,
    val movieWebm: CommunityPublicAssetImageUrl,
    val movieMp4: CommunityPublicAssetImageUrl,
    val movieWebmSmall: CommunityPublicAssetImageUrl,
    val movieMp4Small: CommunityPublicAssetImageUrl,
    val flags: Int,
    val itemId: Long
) {
    internal constructor(proto: steam.webui.player.ProfileItem): this(
        imageSmall = CommunityPublicAssetImageUrl(proto.image_small.orEmpty()),
        imageLarge = CommunityPublicAssetImageUrl(proto.image_large.orEmpty()),
        name = proto.name.orEmpty(),
        itemTitle = proto.item_title.orEmpty(),
        itemDescription = proto.item_description.orEmpty(),
        appId = proto.appid ?: 0,
        itemType = proto.item_type ?: 0,
        itemClass = proto.item_class ?: 0,
        movieWebm = CommunityPublicAssetImageUrl(proto.movie_webm.orEmpty()),
        movieMp4 = CommunityPublicAssetImageUrl(proto.movie_mp4.orEmpty()),
        movieWebmSmall = CommunityPublicAssetImageUrl(proto.movie_webm_small.orEmpty()),
        movieMp4Small = CommunityPublicAssetImageUrl(proto.movie_mp4_small.orEmpty()),
        flags = proto.equipped_flags ?: 0,
        itemId = proto.communityitemid ?: 0
    )
}

fun steam.webui.player.ProfileItem?.toAppModel() = this?.let { ProfileItem(it) }