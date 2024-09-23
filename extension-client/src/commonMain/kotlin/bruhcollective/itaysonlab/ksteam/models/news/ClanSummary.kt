package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClanSummary (
    val success: Int = 0,
    @SerialName("clanAccountID") val accountId: Int = 0,
    @SerialName("clanSteamIDString") val steamId: String = "",
    @SerialName("member_count") val members: Int = 0,
    @SerialName("vanity_url") val vanityUrl: String = "",
    @SerialName("is_creator_home") val isCreator: Int = 0,
    @SerialName("is_curator") val isCurator: Boolean = false,
    @SerialName("has_visible_store_page") val hasStorePage: Boolean = false,
    @SerialName("group_name") val name: String = "",
    @SerialName("avatar_full_url") val avatarFull: String = "",
    @SerialName("avatar_medium_url") val avatarMedium: String = "",
    @SerialName("creator_page_bg_url") val creatorPageBackground: String = "",
    @SerialName("partner_events_enabled") val partnerEventsEnabled: Boolean = false,
)