package bruhcollective.itaysonlab.ksteam.guard.models

import bruhcollective.itaysonlab.ksteam.platform.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Immutable
class MobileConfirmationItem(
    val type: Int,
    @SerialName("type_name") val typeName: String,
    val id: String,
    @SerialName("creator_id") val creatorId: String,
    val nonce: String,
    @SerialName("creation_time") val creationTime: Long,
    @SerialName("cancel") val cancelButtonText: String,
    @SerialName("accept") val acceptButtonText: String,
    val icon: String,
    val multi: Boolean,
    val headline: String,
    val summary: List<String>,
)