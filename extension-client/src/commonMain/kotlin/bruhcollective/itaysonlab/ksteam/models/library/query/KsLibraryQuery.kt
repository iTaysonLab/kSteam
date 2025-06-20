package bruhcollective.itaysonlab.ksteam.models.library.query

import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionAppType
import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionPlayState
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory

data class KsLibraryQuery (
    val appIds: List<Int>,
    val appType: List<ECollectionAppType>,
    val playState: ECollectionPlayState?,
    val storeCategories: List<List<EStoreCategory>>,
    val controllerSupport: KsLibraryQueryControllerSupportFilter,
    val searchQuery: String?,
    val offset: Int,
    val limit: Int,
    val ownerTypeFilter: KsLibraryQueryOwnerFilter,
    val masterSubPackageId: Int,
    val storeTags: List<Int>,
    val steamDeckMinimumSupport: ESteamDeckSupport,
    val sortBy: KsLibraryQuerySortBy,
    val sortByDirection: KsLibraryQuerySortByDirection,
    val fetchFullInformation: Boolean,
    val alwaysFetchLicenses: Boolean,
    val alwaysFetchPlayTime: Boolean,
) {
    fun newBuilder(): KsLibraryQueryBuilder {
        return KsLibraryQueryBuilder(this)
    }
}

