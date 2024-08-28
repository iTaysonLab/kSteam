package bruhcollective.itaysonlab.ksteam.persistence

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.platform.Keychain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * Defines a kSteam persistence driver using Apple's NSUserDefaults for non-secure settings and Keychain for secure K/V pairs.
 *
 * Some important things to consider:
 * - Apple treats NSNumber as 64-bit long on 64-bit executables, and 32-bit in other ways. Because K/Native works only with 64 bits, getLong will be actually equal in everything to getInt.
 * - This stuff is still experimental (like the whole kSteam library). Please, be careful.
 *
 * @param suiteName the name for shared preference file. Generally, you should change it only on macOS and if your application is not sandboxed.
 * @param serviceName the name for Keychain service name. Generally, you should change it only on macOS and if your application is not sandboxed (it should not collide with other applications otherwise).
 * @param allowIdentitySynchronization allow synchronizing Keychain items via iCloud.
 */
class ApplePersistenceDriver (
    private val suiteName: String = "kSteam",
    private val serviceName: String = "kSteam",
    private val allowIdentitySynchronization: Boolean = true
): KsteamPersistenceDriver {
    private val nsDefaults = NSUserDefaults(suiteName = suiteName)

    override fun getString(key: String): String? = nsDefaults.stringForKey(key)
    override fun getLong(key: String): Long = nsDefaults.integerForKey(key)
    override fun getInt(key: String): Int = nsDefaults.integerForKey(key).toInt() // 64-bit apps treat NSNumber as Long

    override fun set(key: String, value: String) { nsDefaults.setObject(forKey = key, value = value) }
    override fun set(key: String, value: Long) { nsDefaults.setInteger(forKey = key, value = value) }
    override fun set(key: String, value: Int) { nsDefaults.setInteger(forKey = key, value = value.toLong()) }

    override fun containsKey(key: String): Boolean = nsDefaults.objectForKey(key) != null
    override fun delete(vararg key: String) { key.forEach(nsDefaults::removeObjectForKey) }

    // Security: we "compact" secure user information into one "application password" that are differentiable by the SteamId

    private val secureIdentities = mutableMapOf<SteamId, MutableMap<String, String>>()

    override fun secureGet(id: SteamId, key: String): String? {
        return getIdentity(id)[key]
    }

    override fun secureSet(id: SteamId, key: String, value: String) {
        getIdentity(id)[key] = value
        uploadIdentity(id)
    }

    override fun secureSet(id: SteamId, vararg pairs: Pair<String, String>) {
        getIdentity(id).putAll(pairs)
        uploadIdentity(id)
    }

    override fun secureDelete(id: SteamId, vararg key: String) {
        getIdentity(id).apply { key.forEach { remove(it) } }
        uploadIdentity(id)
    }

    override fun secureGetSteamIds(): List<SteamId> {
        TODO()
    }

    override fun secureContainsIdentity(id: SteamId): Boolean = getIdentity(id).isNotEmpty()

    override fun secureContainsKey(id: SteamId, key: String): Boolean {
        return getIdentity(id).containsKey(key)
    }

    private fun getIdentity(id: SteamId): MutableMap<String, String> {
        return secureIdentities.getOrPut(id) {
            Keychain.findItem(query = createKeychainQuery(id))?.let {
                Json.decodeFromString<Map<String, String>>(it)
            }.orEmpty().toMutableMap()
        }
    }

    private fun uploadIdentity(id: SteamId) {
        val identity = getIdentity(id)

        if (identity.isNotEmpty()) {
            Keychain.upsertItem(
                query = createKeychainQuery(id, withDescription = true),
                data = Json.encodeToString<Map<String, String>>(getIdentity(id))
            )
        } else {
            Keychain.deleteItem(
                query = createKeychainQuery(id),
            )
        }
    }

    private fun createKeychainQuery(id: SteamId, withDescription: Boolean = false) = Keychain.Query.GenericPassword(
        accountName = id.toString(),
        serviceName = serviceName,
        description = if (withDescription) "kSteam identity data for SteamID $id" else null,
        allowIcloudSynchronization = allowIdentitySynchronization
    )
}
