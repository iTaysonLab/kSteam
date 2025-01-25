package bruhcollective.itaysonlab.ksteam.models.guard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileConfirmationItem(
    @SerialName("type") val type: Int,
    @SerialName("type_name") val typeName: String = "",
    @SerialName("id") val id: String = "",
    @SerialName("creator_id") val creatorId: String = "",
    @SerialName("nonce") val nonce: String = "",
    @SerialName("creation_time") val creationTime: Long = 0L,
    @SerialName("cancel") val cancelButtonText: String = "",
    @SerialName("accept") val acceptButtonText: String = "",
    @SerialName("icon") val icon: String? = null,
    @SerialName("multi") val multi: Boolean = false,
    @SerialName("headline") val headline: String = "",
    @SerialName("summary") val summary: List<String> = emptyList(),
)