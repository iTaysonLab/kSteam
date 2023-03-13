package bruhcollective.itaysonlab.ksteam.database

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.keyvalue.PicsVdfKvDatabase

internal class KSteamDatabase (steamClient: SteamClient) {
    val vdf = PicsVdfKvDatabase(steamClient.config.keyValueDatabase)
}