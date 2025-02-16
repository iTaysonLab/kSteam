package bruhcollective.itaysonlab.ksteam.models.persona

import steam.webui.player.CPlayer_GetProfileItemsEquipped_Response

/**
 * Describes profile equipment - a set of items that can be "equipped" from the Points Shop.
 */
data class ProfileEquipment (
    /**
     * A background that is shown on profile's page.
     */
    val background: ProfileItem? = null,

    /**
     * A mini background that is shown on profile's context menus.
     */
    val miniBackground: ProfileItem? = null,

    /**
     * An avatar frame - a picture that is layered on top of an avatar.
     */
    val avatarFrame: ProfileItem? = null,

    /**
     * An animated avatar - MP4/WebM movie that is displayed instead of an avatar.
     */
    val animatedAvatar: ProfileItem? = null,

    /**
     * Profile modifier - basically, a "theme" that affects colors of widgets on profile page.
     */
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
