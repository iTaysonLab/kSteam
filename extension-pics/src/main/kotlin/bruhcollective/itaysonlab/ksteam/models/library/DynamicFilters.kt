package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*

data class DynamicFilters (
    val byAppType: DfEntry<EAppType> = DfEntry(emptyList<EAppType>() to false),
    val byPlayState: DfEntry<EPlayState> = DfEntry(emptyList<EPlayState>() to false),
    val byAppFeature: DfEntry<EAppFeature> = DfEntry(emptyList<EAppFeature>() to false),
    val byGenre: DfEntry<EGenre> = DfEntry(emptyList<EGenre>() to false),
    val byStoreTag: DfEntry<Int> = DfEntry(emptyList<Int>() to false),
    val byPartner: DfEntry<EPartner> = DfEntry(emptyList<EPartner>() to false),
    val byFriend: DfEntry<SteamId> = DfEntry(emptyList<SteamId>() to false)
)

@JvmInline
value class DfEntry <T> (
    private val packed: Pair<List<T>, Boolean>
) {
    val entries: List<T> get() = packed.first
    val acceptsUnion: Boolean get() = packed.second

    override fun toString() = "DfEntry[union = ${acceptsUnion}, entries = ${entries.joinToString()}]"
}