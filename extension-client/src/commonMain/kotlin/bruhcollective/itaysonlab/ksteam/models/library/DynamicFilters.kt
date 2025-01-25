package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import kotlin.jvm.JvmInline

data class DynamicFilters (
    val byAppType: DfEntry<ECollectionAppType> = DfEntry(emptyList<ECollectionAppType>() to false),
    val byPlayState: DfEntry<ECollectionPlayState> = DfEntry(emptyList<ECollectionPlayState>() to false),
    val byAppFeature: DfEntry<ECollectionAppFeature> = DfEntry(emptyList<ECollectionAppFeature>() to false),
    val byGenre: DfEntry<ECollectionGenre> = DfEntry(emptyList<ECollectionGenre>() to false),
    val byStoreTag: DfEntry<Int> = DfEntry(emptyList<Int>() to false),
    val byPartner: DfEntry<ECollectionPartner> = DfEntry(emptyList<ECollectionPartner>() to false),
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