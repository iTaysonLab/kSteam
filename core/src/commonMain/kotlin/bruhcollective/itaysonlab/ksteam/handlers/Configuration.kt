package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.account.SteamAccountAuthorization
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import bruhcollective.itaysonlab.ksteam.platform.getRandomUuid

/**
 * Provides customized storage implementation for handlers
 */
class Configuration internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    private companion object Keys {
        const val KEY_MACHINE_ID = "machine_id"
        const val KEY_MACHINE_UUID = "machine_uuid"
        const val KEY_AUTOLOGIN_ID = "autologin_steamid"

        const val KEY_SECURE_NAME = "account_name"
        const val KEY_SECURE_ACCESS_TOKEN = "access_token"
        const val KEY_SECURE_REFRESH_TOKEN = "refresh_token"
    }

    private val persist get() = steamClient.config.persistenceDriver

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
        return machineUuid.ifEmpty {
            getRandomUuid().also { machineUuid = it }
        }
    }

    fun containsSecureAccount(id: SteamId): Boolean {
        return id != SteamId.Empty && persist.secureContainsIdentity(id)
    }

    fun getSecureAccount(id: SteamId): SteamAccountAuthorization? {
        return if (persist.secureContainsIdentity(id)) {
            SteamAccountAuthorization(
                accountName = persist.secureGet(id, KEY_SECURE_NAME).orEmpty(),
                accessToken = persist.secureGet(id, KEY_SECURE_ACCESS_TOKEN).orEmpty(),
                refreshToken = persist.secureGet(id, KEY_SECURE_REFRESH_TOKEN).orEmpty(),
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
}