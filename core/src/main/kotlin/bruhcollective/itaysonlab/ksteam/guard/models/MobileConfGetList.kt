package bruhcollective.itaysonlab.ksteam.guard.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ConfirmationListState {
    companion object {
        val Decoder = Json {
            classDiscriminator = "success"
            ignoreUnknownKeys = true
        }
    }

    @SerialName("true")
    class Success(
        val items: List<MobileConfirmationItem>
    ) : ConfirmationListState()

    @SerialName("false")
    class Error(
        val message: String = "",
        val detail: String = ""
    ) : ConfirmationListState()
}