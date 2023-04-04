package bruhcollective.itaysonlab.ksteam.models.persona

import kotlinx.serialization.Serializable

@Serializable
internal data class PlayerSummaries (
    val players: List<PlayerSummary>
)