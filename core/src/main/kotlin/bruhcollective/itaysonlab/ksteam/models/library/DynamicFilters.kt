package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*

data class DynamicFilters (
    val byAppType: List<EAppType>,
    val byPlayState: List<EPlayState>,
    val byAppFeature: List<EAppFeature>,
    val byGenre: List<EGenre>,
    val byStoreTag: List<Int>,
    val byPartner: List<EPartner>,
    val byFriend: List<SteamId>
)