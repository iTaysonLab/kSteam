package bruhcollective.itaysonlab.ksteam.guard.models

import androidx.compose.runtime.Stable
import bruhcollective.itaysonlab.ksteam.guard.MobileConfirmSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable(with = MobileConfirmSerializer::class)
@Stable
sealed class ConfirmationListState {
    companion object {
        val Decoder = Json {
            classDiscriminator = "success"
            ignoreUnknownKeys = true
        }
    }

    @Stable
    object Loading : ConfirmationListState()

    @Stable
    class NetworkError(
        val e: Exception
    ) : ConfirmationListState()

    @Serializable
    @Stable
    class Success(
        val conf: List<MobileConfirmationItem>
    ) : ConfirmationListState()

    @Serializable
    @Stable
    class Error(
        val message: String = "",
        val detail: String = ""
    ) : ConfirmationListState()
}