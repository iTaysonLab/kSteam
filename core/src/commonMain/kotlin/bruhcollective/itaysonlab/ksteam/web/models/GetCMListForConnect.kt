package bruhcollective.itaysonlab.ksteam.web.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetCMListForConnectResponse(
    @SerialName("serverlist") val servers: List<CMServerEntry>
)

@Serializable
class CMServerEntry(
    val endpoint: String,
    val type: String,
    val dc: String,
    val realm: String,
    val load: Int
)