package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.AppId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class LibraryCollection(
    val id: String,
    val name: String,
    val added: List<AppId>,
    val removed: List<AppId>,
    val filterSpec: DynamicFilterSpec?,
    internal val timestamp: Int,
    internal val version: Int,
) {
    fun toRemoteModel() = CollectionModel(id, name, added.map(AppId::id), removed.map(AppId::id), filterSpec)

    @Serializable
    data class DynamicFilterSpec(
        @SerialName("nFormatVersion") val formatVersion: Int,
        @SerialName("strSearchText") val searchText: String,
        @SerialName("filterGroups") val filterGroups: List<FilterGroup>,
        @SerialName("setSuggestions") val suggestions: List<Int>, // TODO
    ) {
        @Serializable
        data class FilterGroup(
            @SerialName("rgOptions") val options: List<Int>,
            @SerialName("bAcceptUnion") val acceptUnion: Boolean
        )
    }

    @Serializable
    data class CollectionModel(
        val id: String,
        val name: String,
        val added: List<Int>,
        val removed: List<Int>,
        val filterSpec: DynamicFilterSpec? = null,
    )

}