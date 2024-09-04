package bruhcollective.itaysonlab.ksteam.models.library.query

import bruhcollective.itaysonlab.ksteam.models.enums.EAppType
import bruhcollective.itaysonlab.ksteam.models.enums.EPlayState
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory

data class KsLibraryQuery internal constructor(
    val appType: List<EAppType>,
    val playState: EPlayState?,
    val storeCategories: List<List<EStoreCategory>>,
    val controllerSupport: KsLibraryQueryControllerSupportFilter,
    val searchQuery: String?,
    val limit: Int,
    val ownerTypeFilter: KsLibraryQueryOwnerFilter,
    val masterSubPackageId: Int,
    val storeTags: List<Int>,
    val steamDeckMinimumSupport: ESteamDeckSupport,
    val sortBy: KsLibraryQuerySortBy,
    val sortByDirection: KsLibraryQuerySortByDirection
)

