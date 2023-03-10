package bruhcollective.itaysonlab.ksteam.models.library

import androidx.compose.runtime.Immutable
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
data class LibraryCollection(
    val id: String,
    val name: String,
    val added: List<AppId> = emptyList(),
    val removed: List<AppId> = emptyList(),
    val filterSpec: DynamicFilterSpec? = null,
    internal val timestamp: Int,
    internal val version: Long,
) {
    internal fun toRemoteModel() = CollectionModel(id, name, added.map(AppId::id), removed.map(AppId::id), filterSpec)

    @Serializable
    @Immutable
    data class DynamicFilterSpec(
        @SerialName("nFormatVersion") val formatVersion: Int = 2,
        @SerialName("strSearchText") val searchText: String = "",
        @SerialName("filterGroups") val filterGroups: List<FilterGroup> = emptyList(),
        @SerialName("setSuggestions") val suggestions: List<Int> = emptyList(), // TODO
    ) {
        @Serializable
        @Immutable
        data class FilterGroup(
            @SerialName("rgOptions") val options: List<Int>,
            @SerialName("bAcceptUnion") val acceptUnion: Boolean
        )

        internal fun parseFilters() = DynamicFilters(
            byAppType = DfEntry(mapOptions(0, EAppType::byBitMask) to acceptsUnion(0)),
            byPlayState = DfEntry(mapOptions(1, EPlayState::byIndex) to acceptsUnion(1)),
            byAppFeature = DfEntry(mapOptions(2, EAppFeature::byIndex) to acceptsUnion(2)),
            byGenre = DfEntry(mapOptions(3, EGenre::byNumber) to acceptsUnion(3)),
            byStoreTag = DfEntry(mapOptions(4) { it } to acceptsUnion(4)),
            byPartner = DfEntry(mapOptions(5, EPartner::byIndex) to acceptsUnion(5)),
            byFriend = DfEntry(emptyList<SteamId>() to acceptsUnion(6)) // TODO
        )

        private fun <T> mapOptions(idx: Int, mapper: (Int) -> T?) = filterGroups.getOrNull(idx)?.options?.mapNotNull(mapper).orEmpty()
        private fun acceptsUnion(idx: Int) = filterGroups.getOrNull(idx)?.acceptUnion ?: false
    }

    @Immutable
    @Serializable
    internal data class CollectionModel(
        val id: String,
        val name: String,
        val added: List<Int> = emptyList(),
        val removed: List<Int> = emptyList(),
        val filterSpec: DynamicFilterSpec? = null,
    )
}