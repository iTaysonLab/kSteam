package bruhcollective.itaysonlab.ksteam.pics

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.persist.Database
import bruhcollective.itaysonlab.ksteam.persist.PicsAppQueries
import bruhcollective.itaysonlab.ksteam.persist.PicsPackageQueries
import bruhcollective.itaysonlab.ksteam.persist.StoreTagQueries

internal class PicsDatabase (steamClient: SteamClient) {
    private val appSchema = Database(steamClient.config.sqlDriver)

    init {
        Database.Schema.create(steamClient.config.sqlDriver)
    }

    val picsAppQueries: PicsAppQueries get() = appSchema.picsAppQueries
    val picsPackageQueries: PicsPackageQueries get() = appSchema.picsPackageQueries
    val storeTagQueries: StoreTagQueries get() = appSchema.storeTagQueries
}