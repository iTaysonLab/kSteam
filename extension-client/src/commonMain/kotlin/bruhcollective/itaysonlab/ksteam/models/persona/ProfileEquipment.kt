package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.platform.Immutable
import steam.webui.player.CPlayer_GetProfileItemsEquipped_Response

@Immutable
data class ProfileEquipment (
    val background: ProfileItem? = null,
    val miniBackground: ProfileItem? = null,
    val avatarFrame: ProfileItem? = null,
    val animatedAvatar: ProfileItem? = null,
    val profileModifier: ProfileItem? = null,
) {
    companion object {
        val None = ProfileEquipment()
    }

    internal constructor(proto: CPlayer_GetProfileItemsEquipped_Response): this(
        background = proto.profile_background.toAppModel(),
        miniBackground = proto.mini_profile_background.toAppModel(),
        avatarFrame = proto.avatar_frame.toAppModel(),
        animatedAvatar = proto.animated_avatar.toAppModel(),
        profileModifier = proto.profile_modifier.toAppModel(),
    )
}
