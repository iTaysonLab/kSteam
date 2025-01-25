package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a library collection.
 *
 * A library collection is a defined set of filters which is used to sort applications in user's Steam library.
 */
sealed interface LibraryCollection {
    val id: String
    val name: String
    val timestamp: Int
    val version: Long

    companion object {
        val Placeholder = Simple("", "", 0, 0, emptyList(), emptyList())

        internal fun fromJsonCollection(remote: RemoteCollectionModel, timestamp: Int, version: Long): LibraryCollection {
            return if (remote.filterSpec != null) {
                Dynamic(
                    id = remote.id,
                    name = remote.name,
                    timestamp = timestamp,
                    version = version,
                    filters = remote.filterSpec.parseFilters()
                )
            } else {
                Simple(
                    id = remote.id,
                    name = remote.name,
                    timestamp = timestamp,
                    version = version,
                    added = remote.added,
                    removed = remote.removed
                )
            }
        }
    }

    /**
     * A simple collection which does not use any sort method.
     *
     * You can add applications here.
     */
    data class Simple(
        override val id: String,
        override val name: String,
        override val timestamp: Int,
        override val version: Long = 0,
        val added: List<Long> = emptyList(),
        val removed: List<Long> = emptyList(),
    ): LibraryCollection

    /**
     * A dynamic collection which can specify a variety of filters.
     */
    data class Dynamic(
        override val id: String,
        override val name: String,
        override val timestamp: Int,
        override val version: Long = 0,
        val filters: DynamicFilters = DynamicFilters(),
    ): LibraryCollection
}

@Serializable
internal data class RemoteCollectionModel(
    val id: String = "",
    val name: String = "",
    val added: List<Long> = emptyList(),
    val removed: List<Long> = emptyList(),
    val filterSpec: DynamicFilterSpec? = null,
)

@Serializable
internal class DynamicFilterSpec(
    @SerialName("nFormatVersion") val formatVersion: Int = 2,
    @SerialName("strSearchText") val searchText: String = "",
    @SerialName("filterGroups") val filterGroups: List<FilterGroup> = emptyList(),
    // @SerialName("setSuggestions") val suggestions: Map<String, String> = emptyMap(), why Valve puts setSuggestions: {} and setSuggestions: [] in different files....
) {
    @Serializable
    internal class FilterGroup(
        @SerialName("rgOptions") val options: List<Int>,
        @SerialName("bAcceptUnion") val acceptUnion: Boolean
    )

    internal fun parseFilters() = DynamicFilters(
        byAppType = DfEntry(mapOptions(0, ECollectionAppType::byBitMask) to acceptsUnion(0)),
        byPlayState = DfEntry(mapOptions(1, ECollectionPlayState::byIndex) to acceptsUnion(1)),
        byAppFeature = DfEntry(mapOptions(2, ECollectionAppFeature::byIndex) to acceptsUnion(2)),
        byGenre = DfEntry(mapOptions(3, ECollectionGenre::byNumber) to acceptsUnion(3)),
        byStoreTag = DfEntry(mapOptions(4) { it } to acceptsUnion(4)),
        byPartner = DfEntry(mapOptions(5, ECollectionPartner::byIndex) to acceptsUnion(5)),
        byFriend = DfEntry(emptyList<SteamId>() to acceptsUnion(6)) // TODO
    )

    private fun <T> mapOptions(idx: Int, mapper: (Int) -> T?) = filterGroups.getOrNull(idx)?.options?.mapNotNull(mapper).orEmpty()
    private fun acceptsUnion(idx: Int) = filterGroups.getOrNull(idx)?.acceptUnion ?: false
}