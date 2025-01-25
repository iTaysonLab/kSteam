package bruhcollective.itaysonlab.ksteam.models.guard

import bruhcollective.itaysonlab.ksteam.guard.MobileConfirmSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable(with = MobileConfirmSerializer::class)
sealed interface ConfirmationListState {
    companion object {
        val Decoder = Json {
            classDiscriminator = "success"
            ignoreUnknownKeys = true
        }
    }

    data object Loading : ConfirmationListState

    class NetworkError(
        val e: Exception
    ) : ConfirmationListState

    @Serializable
    class Success(
        val conf: List<MobileConfirmationItem>
    ) : ConfirmationListState

    @Serializable
    class Error(
        val message: String = "",
        val detail: String = ""
    ) : ConfirmationListState
}