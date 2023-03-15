package bruhcollective.itaysonlab.ksteam.models.persona

import kotlinx.serialization.Serializable

@Serializable
internal data class PlayerSummary(
    val steamid: String,
    val personastate: Int,
    val personaname: String,
    val profileurl: String,
    val avatar: String? = null,
    val avatarmedium: String? = null,
    val avatarfull: String? = null,
    val avatarhash: String? = null,
    val lastlogoff: Int? = null,
    val lastlogon: Int? = null,
    val lastseenonline: Int? = null,
    val timecreated: Long? = null,
    val loccountrycode: String? = null,
    val locstatecode: String? = null,
    val loccityid: Int? = null,
    val primaryclanid: String? = null,
    val realname: String? = null,
    val gameextrainfo: String? = null,
    val gameid: Int? = null,
)
