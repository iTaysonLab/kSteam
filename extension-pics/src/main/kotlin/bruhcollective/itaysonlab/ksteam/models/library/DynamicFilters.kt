package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*

internal data class DynamicFilters (
    val byAppType: DfEntry<EAppType>,
    val byPlayState: DfEntry<EPlayState>,
    val byAppFeature: DfEntry<EAppFeature>,
    val byGenre: DfEntry<EGenre>,
    val byStoreTag: DfEntry<Int>,
    val byPartner: DfEntry<EPartner>,
    val byFriend: DfEntry<SteamId>
)

@JvmInline
internal value class DfEntry <T> (
    private val packed: Pair<List<T>, Boolean>
) {
    val entries: List<T> get() = packed.first
    val acceptsUnion: Boolean get() = packed.second

    override fun toString() = "DfEntry[union = ${acceptsUnion}, entries = ${entries.joinToString()}]"
}