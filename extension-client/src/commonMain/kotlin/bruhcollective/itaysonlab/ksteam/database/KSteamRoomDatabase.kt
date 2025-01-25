package bruhcollective.itaysonlab.ksteam.database

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.room.KsSharedDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsUserDatabase
import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * Manages Room databases.
 */
internal class KSteamRoomDatabase (
    steamClient: SteamClient,
    private val databaseProvider: KSteamDatabaseProvider,
) {
    internal val sharedDatabase: KsSharedDatabase = KsSharedDatabase.newInstance(databaseProvider.createSharedDatabase())

    private var _currentUserId: SteamId = SteamId.Empty
    private var _currentUserDb: KsUserDatabase? = null

    internal val isCurrentUserDatabaseInitialized get() = _currentUserDb != null
    internal val currentUserDatabase: KsUserDatabase get() = _currentUserDb ?: error("User Realm was not yet initialized.")

    init {
        steamClient.configuration.autologinSteamId.takeUnless(SteamId::isEmpty)?.let { autologinSteamId ->
            initializeUserRealm(autologinSteamId)
        }
    }

    internal fun initializeUserRealm(id: SteamId) {
        if (_currentUserId != id) {
            _currentUserId = id
            _currentUserDb?.close()
            _currentUserDb = KsUserDatabase.newInstance(databaseProvider.createUserDatabase(id))
        }
    }
}