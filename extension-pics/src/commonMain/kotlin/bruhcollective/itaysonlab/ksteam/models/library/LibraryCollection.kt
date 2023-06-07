package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EAppFeature
import bruhcollective.itaysonlab.ksteam.models.enums.EAppType
import bruhcollective.itaysonlab.ksteam.models.enums.EGenre
import bruhcollective.itaysonlab.ksteam.models.enums.EPartner
import bruhcollective.itaysonlab.ksteam.models.enums.EPlayState
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a library collection.
 *
 * A library collection is a defined set of filters which is used to sort applications in user's Steam library.
 */
@Immutable
sealed class LibraryCollection (
    val id: String,
    val name: String,
    internal val timestamp: Int,
    internal val version: Long,
) {
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
                    added = remote.added.map(::AppId),
                    removed = remote.removed.map(::AppId)
                )
            }
        }
    }

    /**
     * A simple collection which does not use any sort method.
     *
     * You can add applications here.
     */
    class Simple(
        id: String,
        name: String,
        timestamp: Int,
        version: Long = 0,
        val added: List<AppId> = emptyList(),
        val removed: List<AppId> = emptyList(),
    ): LibraryCollection(id, name, timestamp, version)

    /**
     * A dynamic collection which can specify a variety of filters.
     */
    class Dynamic(
        id: String,
        name: String,
        timestamp: Int,
        version: Long = 0,
        val filters: DynamicFilters = DynamicFilters(),
    ): LibraryCollection(id, name, timestamp, version)
}



@Serializable
internal data class RemoteCollectionModel(
    val id: String = "",
    val name: String = "",
    val added: List<Int> = emptyList(),
    val removed: List<Int> = emptyList(),
    val filterSpec: DynamicFilterSpec? = null,
)

@Serializable
internal class DynamicFilterSpec(
    @SerialName("nFormatVersion") val formatVersion: Int = 2,
    @SerialName("strSearchText") val searchText: String = "",
    @SerialName("filterGroups") val filterGroups: List<FilterGroup> = emptyList(),
    @SerialName("setSuggestions") val suggestions: List<Int> = emptyList(), // TODO
) {
    @Serializable
    internal class FilterGroup(
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