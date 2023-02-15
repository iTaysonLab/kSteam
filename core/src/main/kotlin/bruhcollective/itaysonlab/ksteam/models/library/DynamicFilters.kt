package bruhcollective.itaysonlab.ksteam.models.library

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.*

data class DynamicFilters (
    val byAppType: DfEntry<EAppType>,
    val byPlayState: DfEntry<EPlayState>,
    val byAppFeature: DfEntry<EAppFeature>,
    val byGenre: DfEntry<EGenre>,
    val byStoreTag: DfEntry<Int>,
    val byPartner: DfEntry<EPartner>,
    val byFriend: DfEntry<SteamId>
)

typealias DfEntry <T> = Pair<List<T>, Boolean>