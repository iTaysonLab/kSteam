package bruhcollective.itaysonlab.ksteam.persistence

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * Defines a kSteam persistence driver using Android's SharedPreferences for non-secure settings and AndroidX's EncryptedSharedPreferences for secure K/V pairs.
 *
 * @param context Android application context
 */
class AndroidPersistenceDriver(
    private val context: Context
) : KsteamPersistenceDriver {
    private val defaultSharedPreferences = context.getSharedPreferences("kSteam", Context.MODE_PRIVATE)
    private val secureSharedPreferences = EncryptedSharedPreferences(context, "kSteamSecure", MasterKey.Builder(context).build())

    override fun getString(key: String): String? = defaultSharedPreferences.getString(key, null)
    override fun getLong(key: String): Long = defaultSharedPreferences.getLong(key, 0L)
    override fun getInt(key: String): Int = defaultSharedPreferences.getInt(key, 0)

    override fun set(key: String, value: String) {
        defaultSharedPreferences.edit().putString(key, value).apply()
    }

    override fun set(key: String, value: Long) {
        defaultSharedPreferences.edit().putLong(key, value).apply()
    }

    override fun set(key: String, value: Int) {
        defaultSharedPreferences.edit().putInt(key, value).apply()
    }

    override fun containsKey(key: String): Boolean = defaultSharedPreferences.contains(key)
    override fun delete(vararg key: String) {
        defaultSharedPreferences.edit().apply { key.forEach(::remove) }.apply()
    }

    override fun secureGet(id: SteamId, key: String): String? = secureSharedPreferences.getString(idKey(id, key), null)
    override fun secureSet(id: SteamId, key: String, value: String) {
        secureSharedPreferences.edit().apply {
            putBoolean(idKey(id, "_persist"), true)
            putString(idKey(id, key), value)
        }.apply()
    }

    override fun secureContainsKey(id: SteamId, key: String) = secureSharedPreferences.contains(idKey(id, key))

    override fun secureSet(id: SteamId, vararg pairs: Pair<String, String>) {
        secureSharedPreferences.edit().apply {
            putBoolean(idKey(id, "_persist"), true)

            pairs.forEach { pair ->
                putString(idKey(id, pair.first), pair.second)
            }
        }.apply()
    }

    override fun secureDelete(id: SteamId, vararg key: String) {
        secureSharedPreferences.edit().apply { key.map { idKey(id, it) }.forEach(::remove) }.apply()
    }

    override fun secureContainsIdentity(id: SteamId): Boolean =
        secureSharedPreferences.getBoolean(idKey(id, "_persist"), false)

    private fun idKey(id: SteamId, key: String) = "${id}.$key"
}
