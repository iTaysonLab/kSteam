package bruhcollective.itaysonlab.ksteam.guard.models

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
class MobileConfResult(
    val success: Boolean,
)