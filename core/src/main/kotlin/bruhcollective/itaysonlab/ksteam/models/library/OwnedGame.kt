package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.AppId
import steam.webui.player.CPlayer_GetOwnedGames_Response_Game

data class OwnedGame internal constructor(
    val id: AppId,
    val name: String,
    val recentPlaytime: Int,
    val totalPlaytime: Int,
    val iconUrl: String,
    val logoUrl: String,
    val capsuleFilename: String,
    val hasCommunityVisibleStats: Boolean,
    val hasDlc: Boolean,
    val hasWorkshop: Boolean,
    val hasMarket: Boolean,
    val hasLeaderboards: Boolean,
    val lastLaunched: Long,
    val totalPlaytimeWindows: Int,
    val totalPlaytimeLinux: Int,
    val totalPlaytimeMac: Int,
    val contentDescriptors: List<Int>,
    val sortAs: String
) {
    internal constructor(proto: CPlayer_GetOwnedGames_Response_Game) : this(
        id = AppId(proto.appid ?: 0),
        name = proto.name.orEmpty(),
        recentPlaytime = proto.playtime_2weeks ?: 0,
        totalPlaytime = proto.playtime_forever ?: 0,
        iconUrl = proto.img_icon_url.orEmpty(),
        logoUrl = proto.img_logo_url.orEmpty(),
        capsuleFilename = proto.capsule_filename.orEmpty(),
        hasCommunityVisibleStats = proto.has_community_visible_stats ?: false,
        hasDlc = proto.has_dlc ?: false,
        hasWorkshop = proto.has_workshop ?: false,
        hasMarket = proto.has_market ?: false,
        hasLeaderboards = proto.has_leaderboards ?: false,
        lastLaunched = (proto.rtime_last_played ?: 0).toUInt().toLong(),
        totalPlaytimeWindows = proto.playtime_windows_forever ?: 0,
        totalPlaytimeLinux = proto.playtime_windows_forever ?: 0,
        totalPlaytimeMac = proto.playtime_windows_forever ?: 0,
        contentDescriptors = proto.content_descriptorids,
        sortAs = proto.sort_as.orEmpty()
    )
}