package bruhcollective.itaysonlab.ksteam.guard.models

import bruhcollective.itaysonlab.ksteam.guard.MobileConfirmSerializer
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable(with = MobileConfirmSerializer::class)
@Immutable
sealed interface ConfirmationListState {
    companion object {
        val Decoder = Json {
            classDiscriminator = "success"
            ignoreUnknownKeys = true
        }
    }

    @Immutable
    data object Loading : ConfirmationListState

    @Immutable
    class NetworkError(
        val e: Exception
    ) : ConfirmationListState

    @Serializable
    @Immutable
    class Success(
        val conf: List<MobileConfirmationItem>
    ) : ConfirmationListState

    @Serializable
    @Immutable
    class Error(
        val message: String = "",
        val detail: String = ""
    ) : ConfirmationListState
}