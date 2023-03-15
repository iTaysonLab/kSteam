package bruhcollective.itaysonlab.ksteam.guard.models

import androidx.compose.runtime.Immutable
import bruhcollective.itaysonlab.ksteam.guard.MobileConfirmSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable(with = MobileConfirmSerializer::class)
@Immutable
sealed class ConfirmationListState {
    companion object {
        val Decoder = Json {
            classDiscriminator = "success"
            ignoreUnknownKeys = true
        }
    }

    @Immutable
    object Loading : ConfirmationListState()

    @Immutable
    class NetworkError(
        val e: Exception
    ) : ConfirmationListState()

    @Serializable
    @Immutable
    class Success(
        val conf: List<MobileConfirmationItem>
    ) : ConfirmationListState()

    @Serializable
    @Immutable
    class Error(
        val message: String = "",
        val detail: String = ""
    ) : ConfirmationListState()
}