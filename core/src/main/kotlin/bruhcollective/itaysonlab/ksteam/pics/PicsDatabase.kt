package bruhcollective.itaysonlab.ksteam.pics

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.persist.Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

internal class PicsDatabase (steamClient: SteamClient) {
    private val scopeDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(scopeDispatcher)

    private val appSchema = Database(steamClient.config.sqlDriver)

    init {
        scope.launch {
            Database.Schema.create(steamClient.config.sqlDriver).await()
        }
    }

    suspend fun <T> runOnDatabase(block: suspend Database.() -> T) = withContext(scopeDispatcher) {
        block(appSchema)
    }
}