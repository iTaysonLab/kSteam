package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.guard.models.GuardStructure
import bruhcollective.itaysonlab.ksteam.models.SteamId
import kotlin.jvm.JvmInline

/**
 * Manages Steam Guard instances.
 */
@JvmInline
internal value class GuardStorage(
    private val steamClient: SteamClient,
) {
    private val persist get() = steamClient.persistence

    private companion object {
        const val KEY_PREFIX = "guard"

        const val KEY_SHARED_SECRET = "${KEY_PREFIX}.shared_secret"
        const val KEY_SERIAL_NUMBER = "${KEY_PREFIX}.serial_number"
        const val KEY_REVOCATION_CODE = "${KEY_PREFIX}.revocation_code"
        const val KEY_URI = "${KEY_PREFIX}.uri"
        const val KEY_SERVER_TIME = "${KEY_PREFIX}.server_time"
        const val KEY_ACCOUNT_NAME = "${KEY_PREFIX}.account_name"
        const val KEY_TOKEN_GID = "${KEY_PREFIX}.token_gid"
        const val KEY_IDENTITY_SECRET = "${KEY_PREFIX}.identity_secret"
        const val KEY_SECRET_ONE = "${KEY_PREFIX}.secret_1"
    }

    /**
     * Queries the secure storage for a saved Steam Guard structure.
     *
     * @return [GuardStructure] if the structure is present, otherwise null
     */
    fun queryStructure(steamId: SteamId): GuardStructure? {
        return GuardStructure(
            sharedSecret = persist.secureGet(steamId, KEY_SHARED_SECRET) ?: return null,
            serialNumber = persist.secureGet(steamId, KEY_SERIAL_NUMBER)?.toLongOrNull() ?: return null,
            revocationCode = persist.secureGet(steamId, KEY_REVOCATION_CODE) ?: return null,
            uri = persist.secureGet(steamId, KEY_URI) ?: return null,
            serverTime = persist.secureGet(steamId, KEY_SERVER_TIME)?.toLongOrNull() ?: return null,
            accountName = persist.secureGet(steamId, KEY_ACCOUNT_NAME) ?: return null,
            tokenGid = persist.secureGet(steamId, KEY_TOKEN_GID) ?: return null,
            identitySecret = persist.secureGet(steamId, KEY_IDENTITY_SECRET) ?: return null,
            secretOne = persist.secureGet(steamId, KEY_SECRET_ONE) ?: return null,
        )
    }

    /**
     * Writes a Steam Guard structure into the secure storage.
     */
    fun writeStructure(steamId: SteamId, struct: GuardStructure) {
        persist.secureSet(
            steamId,
            KEY_SHARED_SECRET to struct.sharedSecret,
            KEY_SERIAL_NUMBER to struct.serialNumber.toString(),
            KEY_REVOCATION_CODE to struct.revocationCode,
            KEY_URI to struct.uri,
            KEY_SERVER_TIME to struct.serverTime.toString(),
            KEY_ACCOUNT_NAME to struct.accountName,
            KEY_TOKEN_GID to struct.tokenGid,
            KEY_IDENTITY_SECRET to struct.identitySecret,
            KEY_SECRET_ONE to struct.secretOne,
        )
    }

    /**
     * Deletes Steam Guard structure from the secure storage.
     */
    fun deleteStructure(steamId: SteamId) {
        persist.secureDelete(
            steamId,
            KEY_SHARED_SECRET,
            KEY_SERIAL_NUMBER,
            KEY_REVOCATION_CODE,
            KEY_URI,
            KEY_SERVER_TIME,
            KEY_ACCOUNT_NAME,
            KEY_TOKEN_GID,
            KEY_IDENTITY_SECRET,
            KEY_SECRET_ONE
        )
    }
}