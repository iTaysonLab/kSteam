package bruhcollective.itaysonlab.ksteam.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import bruhcollective.itaysonlab.ksteam.models.SteamId
import java.security.KeyStore

/**
 * Defines a kSteam persistence driver using Android's SharedPreferences for non-secure settings and AndroidX's [EncryptedSharedPreferences] for secure K/V pairs.
 *
 * Developer can disable encryption of preferences, because:
 * - Android's keychain is not very reliable in some OEM ROMs or earlier Android builds (data can be wiped by changing the screen lock settings, for example - see [this Android issue](https://issuetracker.google.com/issues/36983155).
 * - Data are forced to only one device, automatic backup/restore is not possible.
 *
 * Note that flipping secure switch is not supported - data is not transferable between these two modes.
 * Also, kSteam uses a different [MasterKey] alias to not conflict with existing AndroidX Security implementations.
 *
 * @param context Android application context
 * @param secure if the driver should use AndroidX's [EncryptedSharedPreferences] for storing sensitive data
 */
class AndroidPersistenceDriver(
    context: Context,
    secure: Boolean = true
) : KsteamPersistenceDriver {
    private companion object {
        const val PREFERENCES_DEFAULT = "kSteam"
        const val PREFERENCES_SECURE = "kSteamSecure"
        const val PREFERENCES_SECURE_FALLBACK = "kSteamSecureFallback"
        const val MASTER_KEY_ALIAS = "kSteamPersistenceMasterKey"
    }

    private val defaultSharedPreferences = context.getSharedPreferences(PREFERENCES_DEFAULT, Context.MODE_PRIVATE)

    private val secureSharedPreferences = if (secure) {
        tryCreateSecureSharedPreferences(context)
    } else {
        createInsecureSharedPreferences(context)
    }

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

    override fun secureGetSteamIds(): List<SteamId> {
        return secureSharedPreferences.all.keys.mapNotNull { it.split(".").firstOrNull()?.toULongOrNull()?.let(::SteamId) }.distinct()
    }

    override fun containsKey(key: String): Boolean = defaultSharedPreferences.contains(key)
    override fun delete(vararg key: String) {
        defaultSharedPreferences.edit().apply { key.forEach(::remove) }.apply()
    }

    override fun secureGet(id: SteamId, key: String): String? = secureSharedPreferences.getString(idKey(id, key), null)
    override fun secureSet(id: SteamId, key: String, value: String) {
        secureSharedPreferences.edit().apply {
            putString(idKey(id, key), value)
        }.apply()
    }

    override fun secureContainsKey(id: SteamId, key: String) = secureSharedPreferences.contains(idKey(id, key))

    override fun secureSet(id: SteamId, vararg pairs: Pair<String, String>) {
        secureSharedPreferences.edit().apply {
            pairs.forEach { pair ->
                putString(idKey(id, pair.first), pair.second)
            }
        }.apply()
    }

    override fun secureDelete(id: SteamId, vararg key: String) {
        secureSharedPreferences.edit().apply { key.map { idKey(id, it) }.forEach(::remove) }.apply()
    }

    private fun idKey(id: SteamId, key: String) = "${id}.$key"

    //

    private fun tryCreateSecureSharedPreferences(context: Context): SharedPreferences {
        return try {
            // On most cases, this should work fine
            createSecureSharedPreferences(context)
        } catch (e: Exception) {
            // If not, we CLEAR the preferences (because they are already not accessible)
            try {
                clearEncryptedPrefs(context)
                createSecureSharedPreferences(context)
            } catch (e: Exception) {
                // Encryption is broken AF, fall back to something that works
                createInsecureSharedPreferences(context)
            }
        }
    }

    private fun clearEncryptedPrefs(context: Context) {
        // Deleting global MasterKey
        with(KeyStore.getInstance("AndroidKeyStore")) {
            load(null)
            deleteEntry(MASTER_KEY_ALIAS)
        }

        // Clearing AndroidX data
        context.getSharedPreferences(PREFERENCES_SECURE, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun createSecureSharedPreferences(context: Context): SharedPreferences {
        return EncryptedSharedPreferences(
            context,
            PREFERENCES_SECURE,
            MasterKey.Builder(context, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        )
    }

    private fun createInsecureSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_SECURE_FALLBACK, Context.MODE_PRIVATE)
    }
}
