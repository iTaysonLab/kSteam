package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class LibraryCollection(
    val id: String,
    val name: String,
    val added: List<AppId>,
    val removed: List<AppId>,
    val filterSpec: DynamicFilterSpec?,
    internal val timestamp: Int,
    internal val version: Long,
) {
    fun toRemoteModel() = CollectionModel(id, name, added.map(AppId::asInt), removed.map(AppId::asInt), filterSpec)

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

        fun parseFilters() = DynamicFilters(
            byAppType = mapOptions(0, EAppType::byBitMask) to acceptsUnion(0),
            byPlayState = mapOptions(1, EPlayState::byIndex) to acceptsUnion(1),
            byAppFeature = mapOptions(2, EAppFeature::byIndex) to acceptsUnion(2),
            byGenre = mapOptions(3, EGenre::byNumber) to acceptsUnion(3),
            byStoreTag = mapOptions(4) { it } to acceptsUnion(4),
            byPartner = mapOptions(5, EPartner::byIndex) to acceptsUnion(5),
            byFriend = emptyList<SteamId>() to acceptsUnion(6) // TODO
        )

        private fun <T> mapOptions(idx: Int, mapper: (Int) -> T?) = filterGroups.getOrNull(idx)?.options?.mapNotNull(mapper).orEmpty()
        private fun acceptsUnion(idx: Int) = filterGroups.getOrNull(idx)?.acceptUnion ?: false
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