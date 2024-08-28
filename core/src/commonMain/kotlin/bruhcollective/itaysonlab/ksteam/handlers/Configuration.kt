package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.account.SteamAccountAuthorization
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import bruhcollective.itaysonlab.ksteam.platform.getRandomUuid

/**
 * Provides a centralized storage implementation for handlers, backed by client's persistence driver.
 */
class Configuration internal constructor(
    private val steamClient: SteamClient
) {
    private companion object Keys {
        const val KEY_MACHINE_ID = "machine_id"
        const val KEY_MACHINE_UUID = "machine_uuid"
        const val KEY_AUTOLOGIN_ID = "autologin_steamid"

        const val KEY_SECURE_NAME = "account_name"
        const val KEY_SECURE_ACCESS_TOKEN = "access_token"
        const val KEY_SECURE_REFRESH_TOKEN = "refresh_token"
        const val KEY_SECURE_DISPLAY_NAME = "display_name"
        const val KEY_SECURE_AVATAR_URL = "avatar_url"

        const val KEY_CELL_ID = "cell_id"
    }

    private val persist get() = steamClient.config.persistenceDriver

    var cellId: Int
        get() = persist.getInt(KEY_CELL_ID)
        set(value) { persist.set(KEY_CELL_ID, value) }

    var machineId: String
        get() = persist.getString(KEY_MACHINE_ID).orEmpty()
        set(value) { persist.set(KEY_MACHINE_ID, value) }

    var machineUuid: String
        get() = persist.getString(KEY_MACHINE_UUID).orEmpty()
        set(value) { persist.set(KEY_MACHINE_UUID, value) }

    var autologinSteamId: SteamId
        get() = persist.getLong(KEY_AUTOLOGIN_ID).toSteamId()
        set(value) { persist.set(KEY_AUTOLOGIN_ID, value.id.toLong()) }

    //

    fun getUuid(): String {
        return "android:" + machineUuid.ifEmpty {
            getRandomUuid().also { machineUuid = it }
        }
    }

    fun containsSecureAccount(id: SteamId): Boolean {
        return id != SteamId.Empty && persist.secureContainsIdentity(id) && persist.secureContainsKey(id, KEY_SECURE_ACCESS_TOKEN) && persist.secureContainsKey(id, KEY_SECURE_REFRESH_TOKEN)
    }

    fun getValidSecureAccountIds(): List<SteamId> {
        return persist.secureGetSteamIds().filter { id ->
            persist.secureContainsKey(id, KEY_SECURE_ACCESS_TOKEN) && persist.secureContainsKey(id, KEY_SECURE_REFRESH_TOKEN)
        }
    }

    fun getSecureAccount(id: SteamId): SteamAccountAuthorization? {
        return if (id == SteamId.Empty) {
            null
        } else if (persist.secureContainsIdentity(id)) {
            SteamAccountAuthorization(
                accountName = persist.secureGet(id, KEY_SECURE_NAME).orEmpty(),
                accessToken = persist.secureGet(id, KEY_SECURE_ACCESS_TOKEN).orEmpty().ifEmpty { return null },
                refreshToken = persist.secureGet(id, KEY_SECURE_REFRESH_TOKEN).orEmpty().ifEmpty { return null },
            )
        } else {
            null
        }
    }

    fun updateSecureAccount(id: SteamId, obj: SteamAccountAuthorization) {
        persist.secureSet(
            id = id,
            KEY_SECURE_NAME to obj.accountName,
            KEY_SECURE_ACCESS_TOKEN to obj.accessToken,
            KEY_SECURE_REFRESH_TOKEN to obj.refreshToken
        )
    }

    fun deleteSecureAccount(id: SteamId) {
        persist.secureDelete(id, KEY_SECURE_NAME, KEY_SECURE_ACCESS_TOKEN, KEY_SECURE_REFRESH_TOKEN)
    }
}