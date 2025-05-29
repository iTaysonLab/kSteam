package examples

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionAppType
import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionGenre
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQueryBuilder
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQueryOwnerFilter
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQuerySortBy
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQuerySortByDirection

object KsQuery {
    suspend fun execute(
        steam: ExtendedSteamClient
    ) {
        val query = KsLibraryQueryBuilder()
            .withAppType(ECollectionAppType.Game)
            .withGenre(ECollectionGenre.Action)
            .withGenre(ECollectionGenre.Adventure)
            .withStoreCategory(EStoreCategory.TradingCard)
            .withStoreCategory(EStoreCategory.GamepadPreferred)
            .withStoreCategories(EStoreCategory.PS5ControllerSupport, EStoreCategory.PS5ControllerBTSupport)
            .withOwnerFilter(KsLibraryQueryOwnerFilter.OwnedOnly)
            .sortBy(KsLibraryQuerySortBy.Name, KsLibraryQuerySortByDirection.Ascending)
            .fetchFullInformation(true)

        steam.library.execute(query.build()).also {
            println(it)
        }.forEach {
            println("====")
            println("Application: [${it.application.id}] ${it.application.name}")
            println("Purchased: [${it.ownsThisApp(steam)}]")
            println("Licenses: [${it.licenses.joinToString { l -> "${l.owner} - ${l.paymentMethod}" }}]")
            println("Play Time: [${it.playTime}]")
        }
    }
}