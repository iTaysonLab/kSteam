package bruhcollective.itaysonlab.ksteam.models.persona

import steam.webui.player.CPlayer_GetProfileItemsEquipped_Response

data class ProfileEquipment internal constructor(
    val background: ProfileItem? = null,
    val miniBackground: ProfileItem? = null,
    val avatarFrame: ProfileItem? = null,
    val animatedAvatar: ProfileItem? = null,
    val profileModifier: ProfileItem? = null,
) {
    internal constructor(proto: CPlayer_GetProfileItemsEquipped_Response): this(
        background = proto.profile_background.toAppModel(),
        miniBackground = proto.mini_profile_background.toAppModel(),
        avatarFrame = proto.avatar_frame.toAppModel(),
        animatedAvatar = proto.animated_avatar.toAppModel(),
        profileModifier = proto.profile_modifier.toAppModel(),
    )
}
