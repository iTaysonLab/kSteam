package bruhcollective.itaysonlab.ksteam.database

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.entities.PicsApp
import bruhcollective.itaysonlab.ksteam.database.entities.PicsPackage
import bruhcollective.itaysonlab.ksteam.database.entities.StoreTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File

internal class KSteamDatabase (
    private val steamClient: SteamClient
) {
    private lateinit var database: Database

    suspend fun <T> runTransaction(block: suspend Transaction.() -> T): T {
        return newSuspendedTransaction(db = database, statement = block)
    }

    suspend fun <T> withDatabase(block: suspend Database.() -> T): T {
        return database.block()
    }

    suspend fun tryInitializeDatabase() = withContext(Dispatchers.IO) {
        if (::database.isInitialized.not()) {
            database = Database.connect("jdbc:h2:${File(steamClient.config.rootFolder, "ksteam").absolutePath}")
        }

        newSuspendedTransaction(db = database) {
            SchemaUtils.createMissingTablesAndColumns(PicsApp, PicsPackage, StoreTag, inBatch = true)
        }
    }
}