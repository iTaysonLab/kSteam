package bruhcollective.itaysonlab.ksteam.web.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class QueryTimeData(
    @SerialName("server_time") val serverTime: Long,
    @SerialName("allow_correction") val allowCorrection: Boolean = false,
)