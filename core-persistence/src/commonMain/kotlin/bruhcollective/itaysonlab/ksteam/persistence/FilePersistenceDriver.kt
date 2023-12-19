/*package bruhcollective.itaysonlab.ksteam.persistence

import bruhcollective.itaysonlab.ksteam.models.SteamId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Path

/**
 * Defines a kSteam persistence driver using a file located in the kSteam working directory.
 *
 * **WARNING: THIS DOES NOT ENCRYPT SECURE DATA!** This includes Steam Guard and Authorization information.
 *
 * @param suiteName the name for shared preference file. Generally, you should change it only on macOS and if your application is not sandboxed.
 * @param serviceName the name for Keychain service name. Generally, you should change it only on macOS and if your application is not sandboxed (it should not collide with other applications otherwise).
 * @param allowIdentitySynchronization allow synchronizing Keychain items via iCloud.
 */
class FilePersistenceDriver (
    private val path: Path
): KsteamPersistenceDriver {

}*/
