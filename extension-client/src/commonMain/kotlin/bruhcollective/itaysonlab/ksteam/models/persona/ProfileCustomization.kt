package bruhcollective.itaysonlab.ksteam.models.persona

import steam.enums.EBanContentCheckResult
import steam.enums.EProfileCustomizationStyle
import steam.enums.EProfileCustomizationType

data class ProfileCustomization (
    val profileWidgets: List<ProfileWidget> = emptyList(),
    val slotsAvailable: Int = 0,
    val profileTheme: ProfileTheme? = null,
    val profilePreferences: ProfilePreferences? = null
) {
    companion object {
        val None = ProfileCustomization()
    }
}

data class ProfileCustomizationEntry (
    val customizationType: EProfileCustomizationType,
    val level: Int,
    val active: Boolean,
    val large: Boolean,
    val style: EProfileCustomizationStyle,
    val slots: List<ProfileCustomizationSlot>
) {
    internal constructor(proto: steam.webui.player.ProfileCustomization): this(
        customizationType = EProfileCustomizationType.fromValue(proto.customization_type ?: -1) ?: error("Unknown CustomizationType"),
        level = proto.level ?: 0,
        active = proto.active ?: false,
        large = proto.large ?: false,
        style = EProfileCustomizationStyle.fromValue(proto.customization_style ?: 0) ?: EProfileCustomizationStyle.k_EProfileCustomizationStyleDefault,
        slots = proto.slots.map { ProfileCustomizationSlot(it) },
    )
}

data class ProfileCustomizationSlot (
    val appId: Int,
    val publishedFileId: Long,
    val itemAssetId: Long,
    val itemContextId: Long,
    val notes: String,
    val title: String,
    val accountId: Int,
    val badgeId: Int,
    val borderColor: Int,
    val itemClassId: Long,
    val itemInstanceId: Long,
    val banResult: EBanContentCheckResult
) {
    internal constructor(proto: steam.webui.player.ProfileCustomizationSlot): this(
        appId = proto.appid ?: 0,
        publishedFileId = proto.publishedfileid ?: 0,
        itemAssetId = proto.item_assetid ?: 0,
        itemContextId = proto.item_contextid ?: 0,
        notes = proto.notes.orEmpty(),
        title = proto.title.orEmpty(),
        accountId = proto.accountid ?: 0,
        badgeId = proto.badgeid ?: 0,
        borderColor = proto.border_color ?: 0,
        itemClassId = proto.item_classid ?: 0,
        itemInstanceId = proto.item_instanceid ?: 0,
        banResult = EBanContentCheckResult.fromValue(proto.ban_check_result ?: 0) ?: EBanContentCheckResult.k_EBanContentCheckResult_NotScanned,
    )
}

data class ProfileTheme (
    val themeId: String,
    val title: String
) {
    internal constructor(proto: steam.webui.player.ProfileTheme): this(
        themeId = proto.theme_id.orEmpty(),
        title = proto.title.orEmpty()
    )
}

data class ProfilePreferences (
    val hideProfileAwards: Boolean
) {
    internal constructor(proto: steam.webui.player.ProfilePreferences): this(
        hideProfileAwards = proto.hide_profile_awards ?: false
    )
}
