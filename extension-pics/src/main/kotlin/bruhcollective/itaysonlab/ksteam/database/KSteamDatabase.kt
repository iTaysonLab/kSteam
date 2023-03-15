package bruhcollective.itaysonlab.ksteam.database

import bruhcollective.itaysonlab.ksteam.database.keyvalue.KeyValueDatabase
import bruhcollective.itaysonlab.ksteam.database.keyvalue.PicsVdfKvDatabase

internal class KSteamDatabase (keyValueDatabase: KeyValueDatabase) {
    val vdf = PicsVdfKvDatabase(keyValueDatabase)
}