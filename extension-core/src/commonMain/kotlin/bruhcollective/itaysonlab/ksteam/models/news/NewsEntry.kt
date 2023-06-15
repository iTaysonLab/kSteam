package bruhcollective.itaysonlab.ksteam.models.news


import bruhcollective.itaysonlab.ksteam.platform.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class NewsEntry internal constructor(
    @SerialName("announcement_body")
    val announcementBody: AnnouncementBody = AnnouncementBody(),
    @SerialName("appid")
    val appid: Int = 0,
    @SerialName("broadcaster_accountid")
    val broadcasterAccountid: Int = 0,
    @SerialName("build_branch")
    val buildBranch: String = "",
    @SerialName("build_id")
    val buildId: Int = 0,
    @SerialName("clan_steamid")
    val clanSteamid: String = "",
    @SerialName("clan_steamid_original")
    val clanSteamidOriginal: String = "",
    @SerialName("comment_count")
    val commentCount: Int = 0,
    @SerialName("comment_type")
    val commentType: String = "",
    @SerialName("creator_steamid")
    val creatorSteamid: String = "",
    @SerialName("event_name")
    val eventName: String = "",
    @SerialName("event_notes")
    val eventNotes: String = "",
    @SerialName("event_type")
    val eventType: Int = 0,
    @SerialName("featured_app_tagid")
    val featuredAppTagid: Int = 0,
    @SerialName("follower_count")
    val followerCount: Int = 0,
    @SerialName("forum_topic_id")
    val forumTopicId: String = "",
    @SerialName("gid")
    val gid: String = "",
    @SerialName("gidfeature")
    val gidfeature: String = "",
    @SerialName("gidfeature2")
    val gidfeature2: String = "",
    @SerialName("hidden")
    val hidden: Int = 0,
    @SerialName("ignore_count")
    val ignoreCount: Int = 0,
    @SerialName("jsondata")
    val jsondata: String = "{}",
    @SerialName("last_update_steamid")
    val lastUpdateSteamid: String = "",
    @SerialName("news_post_gid")
    val newsPostGid: String = "",
    @SerialName("published")
    val published: Int = 0,
    @SerialName("referenced_appids")
    val referencedAppids: List<Int> = emptyList(),
    @SerialName("rtime32_end_time")
    val rtime32EndTime: Int = 0,
    @SerialName("rtime32_last_modified")
    val rtime32LastModified: Int = 0,
    @SerialName("rtime32_start_time")
    val rtime32StartTime: Int = 0,
    @SerialName("rtime32_visibility_end")
    val rtime32VisibilityEnd: Int = 0,
    @SerialName("rtime32_visibility_start")
    val rtime32VisibilityStart: Int = 0,
    @SerialName("rtime_mod_reviewed")
    val rtimeModReviewed: Int = 0,
    @SerialName("server_address")
    val serverAddress: String = "",
    @SerialName("server_password")
    val serverPassword: String = "",
    @SerialName("votes_down")
    val votesDown: Int = 0,
    @SerialName("votes_up")
    val votesUp: Int = 0,
    @SerialName("video_preview_id")
    val videoPreviewId: String = "",
    @SerialName("video_preview_type")
    val videoPreviewType: String = "", // youtube
) {
    @Serializable
    @Immutable
    data class AnnouncementBody(
        @SerialName("ban_check_result")
        val banCheckResult: Int = 0,
        @SerialName("banned")
        val banned: Int = 0,
        @SerialName("body")
        val body: String = "",
        @SerialName("clanid")
        val clanid: String = "",
        @SerialName("commentcount")
        val commentcount: Int = 0,
        @SerialName("event_gid")
        val eventGid: String = "",
        @SerialName("forum_topic_id")
        val forumTopicId: String = "",
        @SerialName("gid")
        val gid: String = "",
        @SerialName("headline")
        val headline: String = "",
        @SerialName("hidden")
        val hidden: Int = 0,
        @SerialName("language")
        val language: Int = 0,
        @SerialName("posterid")
        val posterid: String = "",
        @SerialName("posttime")
        val posttime: Int = 0,
        @SerialName("tags")
        val tags: List<String> = emptyList(),
        @SerialName("updatetime")
        val updatetime: Int = 0,
        @SerialName("votedowncount")
        val votedowncount: Int = 0,
        @SerialName("voteupcount")
        val voteupcount: Int = 0
    )
}