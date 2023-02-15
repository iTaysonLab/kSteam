package bruhcollective.itaysonlab.ksteam.models.news


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsEntry(
    @SerialName("announcement_body")
    val announcementBody: AnnouncementBody,
    @SerialName("appid")
    val appid: Int,
    @SerialName("broadcaster_accountid")
    val broadcasterAccountid: Int,
    @SerialName("build_branch")
    val buildBranch: String,
    @SerialName("build_id")
    val buildId: Int,
    @SerialName("clan_steamid")
    val clanSteamid: String,
    @SerialName("clan_steamid_original")
    val clanSteamidOriginal: String,
    @SerialName("comment_count")
    val commentCount: Int,
    @SerialName("comment_type")
    val commentType: String,
    @SerialName("creator_steamid")
    val creatorSteamid: String,
    @SerialName("event_name")
    val eventName: String,
    @SerialName("event_notes")
    val eventNotes: String,
    @SerialName("event_type")
    val eventType: Int,
    @SerialName("featured_app_tagid")
    val featuredAppTagid: Int,
    @SerialName("follower_count")
    val followerCount: Int,
    @SerialName("forum_topic_id")
    val forumTopicId: String,
    @SerialName("gid")
    val gid: String,
    @SerialName("gidfeature")
    val gidfeature: String,
    @SerialName("gidfeature2")
    val gidfeature2: String,
    @SerialName("hidden")
    val hidden: Int,
    @SerialName("ignore_count")
    val ignoreCount: Int,
    @SerialName("jsondata")
    val jsondata: String,
    @SerialName("last_update_steamid")
    val lastUpdateSteamid: String,
    @SerialName("news_post_gid")
    val newsPostGid: String,
    @SerialName("published")
    val published: Int,
    @SerialName("referenced_appids")
    val referencedAppids: List<Int>,
    @SerialName("rtime32_end_time")
    val rtime32EndTime: Int,
    @SerialName("rtime32_last_modified")
    val rtime32LastModified: Int,
    @SerialName("rtime32_start_time")
    val rtime32StartTime: Int,
    @SerialName("rtime32_visibility_end")
    val rtime32VisibilityEnd: Int,
    @SerialName("rtime32_visibility_start")
    val rtime32VisibilityStart: Int,
    @SerialName("rtime_mod_reviewed")
    val rtimeModReviewed: Int,
    @SerialName("server_address")
    val serverAddress: String,
    @SerialName("server_password")
    val serverPassword: String,
    @SerialName("votes_down")
    val votesDown: Int,
    @SerialName("votes_up")
    val votesUp: Int
) {
    @Serializable
    data class AnnouncementBody(
        @SerialName("ban_check_result")
        val banCheckResult: Int,
        @SerialName("banned")
        val banned: Int,
        @SerialName("body")
        val body: String,
        @SerialName("clanid")
        val clanid: String,
        @SerialName("commentcount")
        val commentcount: Int,
        @SerialName("event_gid")
        val eventGid: String,
        @SerialName("forum_topic_id")
        val forumTopicId: String,
        @SerialName("gid")
        val gid: String,
        @SerialName("headline")
        val headline: String,
        @SerialName("hidden")
        val hidden: Int,
        @SerialName("language")
        val language: Int,
        @SerialName("posterid")
        val posterid: String,
        @SerialName("posttime")
        val posttime: Int,
        @SerialName("tags")
        val tags: List<String>,
        @SerialName("updatetime")
        val updatetime: Int,
        @SerialName("votedowncount")
        val votedowncount: Int,
        @SerialName("voteupcount")
        val voteupcount: Int
    )
}