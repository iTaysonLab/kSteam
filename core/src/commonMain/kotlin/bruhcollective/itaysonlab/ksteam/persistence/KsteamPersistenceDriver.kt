package bruhcollective.itaysonlab.ksteam.persistence

import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * Defines platform-based persistence driver backend used by [KsteamPersistence].
 *
 * What is considered "persisted":
 * - user access information (access/refresh tokens, SteamIDs)
 * - Steam Guard profiles
 * - kSteam runtime information (timezone, machine ID)
 *
 * Other extensions can read or write any information from it.
 */
interface KsteamPersistenceDriver {
    /**
     * Gets an arbitrary key/value item in shared storage. Defaults to null if key is not defined in the storage.
     */
    fun getString(key: String): String?

    /**
     * Gets an arbitrary key/value item in shared storage. Defaults to 0 if key is not defined in the storage.
     */
    fun getLong(key: String): Long

    /**
     * Gets an arbitrary key/value item in shared storage. Defaults to 0 if key is not defined in the storage.
     */
    fun getInt(key: String): Int

    /**
     * Puts an arbitrary key/value item to shared storage. This information is not encrypted in persistence storage.
     */
    fun set(key: String, value: String)

    /**
     * Puts an arbitrary key/value item to shared storage. This information is not encrypted in persistence storage.
     */
    fun set(key: String, value: Long)

    /**
     * Puts an arbitrary key/value item to shared storage. This information is not encrypted in persistence storage.
     */
    fun set(key: String, value: Int)

    /**
     * Checks if key exists in shared storage.
     */
    fun containsKey(key: String): Boolean

    /**
     * Deletes keys from shared storage.
     */
    fun delete(vararg key: String)

    /**
     * Securely returns the list of registered SteamIDs.
     *
     * Not always secure - this is defined by implementation.
     */
    fun secureGetSteamIds(): List<SteamId>

    /**
     * Securely accesses account information K/V for a specific SteamID.
     *
     * Example: `secureGet(SteamId(0u), "access_token")`
     *
     * Not always secure - this is defined by implementation.
     */
    fun secureGet(id: SteamId, key: String): String?

    /**
     * Securely puts account information K/V for a specific SteamID.
     *
     * Example: `securePut(SteamId(0u), "access_token", "_information_")`
     *
     * Not always secure - this is defined by implementation.
     */
    fun secureSet(id: SteamId, key: String, value: String)

    /**
     * Securely puts account information K/V for a specific SteamID.
     *
     * Example: `securePut(SteamId(0u), "access_token" to "_information_", "refresh_token" to "_information2_")`
     *
     * Not always secure - this is defined by implementation.
     */
    fun secureSet(id: SteamId, vararg pairs: Pair<String, String>)

    /**
     * Securely deletes account information K/V for a specific SteamID.
     *
     * Example: `secureDelete(SteamId(0u), "access_token", "refresh_token")`
     *
     * Not always secure - this is defined by implementation.
     */
    fun secureDelete(id: SteamId, vararg key: String)

    /**
     * Checks if identity exists in secure storage.
     *
     * Example: `secureContainsIdentity(SteamId(0u))`
     */
    fun secureContainsIdentity(id: SteamId): Boolean = secureGetSteamIds().contains(id)

    /**
     * Checks if key exists in secure storage for a specific SteamID.
     *
     * Example: `secureContainsKey(SteamId(0u), "access_token")`
     */
    fun secureContainsKey(id: SteamId, key: String): Boolean
}

/**
 * Default persistence implementation. Acts as RAM: completely resets the state on process death.
 */
object MemoryPersistenceDriver: KsteamPersistenceDriver {
    private val map = mutableMapOf<String, String>()

    override fun getString(key: String): String? = map[key]
    override fun getLong(key: String): Long = map[key]?.toLongOrNull() ?: 0L
    override fun getInt(key: String): Int = map[key]?.toIntOrNull() ?: 0

    override fun set(key: String, value: String) { map[key] = value }
    override fun set(key: String, value: Long) { map[key] = value.toString() }
    override fun set(key: String, value: Int) { map[key] = value.toString() }

    override fun containsKey(key: String): Boolean = map.containsKey(key)
    override fun delete(vararg key: String) { key.forEach(map::remove) }

    override fun secureGetSteamIds(): List<SteamId> = map.keys.mapNotNull { it.split(".").getOrNull(1)?.toULongOrNull()?.let(::SteamId) }.distinct()
    override fun secureGet(id: SteamId, key: String): String? = getString("secure.${id}.$key")
    override fun secureSet(id: SteamId, key: String, value: String) { set("secure.${id}.$key", value) }
    override fun secureSet(id: SteamId, vararg pairs: Pair<String, String>) { map.putAll(pairs.map { "secure.${id}.${it.first}" to it.second }) }
    override fun secureDelete(id: SteamId, vararg key: String) { key.map { "secure.${id}.$key" }.forEach(map::remove) }
    override fun secureContainsKey(id: SteamId, key: String): Boolean = containsKey("secure.${id}.$key")
}