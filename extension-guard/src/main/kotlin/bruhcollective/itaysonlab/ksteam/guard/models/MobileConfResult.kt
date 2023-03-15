package bruhcollective.itaysonlab.ksteam.guard.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
class MobileConfResult(
    val success: Boolean,
)