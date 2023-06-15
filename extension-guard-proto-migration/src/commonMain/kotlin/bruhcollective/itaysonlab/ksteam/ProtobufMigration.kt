package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.guard.models.GuardStructure
import bruhcollective.itaysonlab.ksteam.handlers.Guard
import bruhcollective.itaysonlab.ksteam.handlers.storage
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.platform.provideOkioFilesystem
import bruhcollective.itaysonlab.ksteam.proto.GuardConfiguration

private const val LOG_TAG = "GuardMigration"

/**
 * Tries to migrate Steam Guard files from old format to new one.
 *
 * It is safe to call it right after kSteam initialization (even before [SteamClient.start] call)
 *
 * @param clientRef reference to [SteamClient]
 * @param testRun output info to logs and do not anything (no removal/transfer)
 * @param removeOldFiles delete protobuf data after successfully migrating it
 */
@Suppress("SpellCheckingInspection")
suspend fun Guard.tryMigratingProtobufs(
    clientRef: SteamClient,
    testRun: Boolean = false,
    removeOldFiles: Boolean = true
) {
    KSteamLogging.logDebug(LOG_TAG) { "Preparing to migrate SG data" }

    val fs = provideOkioFilesystem()
    val dataStorage = clientRef.storage.rootFolder

    val directoriesWithGuard = fs.list(dataStorage).filter { directory ->
        fs.exists(directory / "guard")
    }

    if (directoriesWithGuard.isEmpty()) {
        KSteamLogging.logDebug(LOG_TAG) { "No files to migrate, aborting" }
        return
    }

    KSteamLogging.logDebug(LOG_TAG) { "Available directories: ${directoriesWithGuard.joinToString()}" }

    directoriesWithGuard.forEach { directory ->
        val guardConfig = fs.read(directory / "guard") {
            GuardConfiguration.ADAPTER.decode(this)
        }

        KSteamLogging.logDebug(LOG_TAG) { "Migrating configuration for user ${guardConfig.steam_id}" }

        if (testRun) {
            KSteamLogging.logDebug(LOG_TAG) {
                "Test run, skipping migration for user ${guardConfig.steam_id}"
            }
        } else {
            tryAddConfig(
                SteamId(guardConfig.steam_id.toULong()), GuardStructure(
                    sharedSecret = guardConfig.shared_secret.base64(),
                    serialNumber = guardConfig.serial_number,
                    revocationCode = guardConfig.revocation_code,
                    uri = guardConfig.uri,
                    serverTime = guardConfig.server_time,
                    accountName = guardConfig.account_name,
                    tokenGid = guardConfig.token_gid,
                    identitySecret = guardConfig.identity_secret.base64(),
                    secretOne = guardConfig.secret_1.base64(),
                    steamId = guardConfig.steam_id,
                )
            )

            if (removeOldFiles) {
                fs.delete(directory / "guard")

                KSteamLogging.logDebug(LOG_TAG) {
                    "Deleted old configuration for user ${guardConfig.steam_id}"
                }
            }
        }

        KSteamLogging.logDebug(LOG_TAG) {
            "Completed configuration migration for user ${guardConfig.steam_id}"
        }
    }

    KSteamLogging.logDebug(LOG_TAG) { "Migration completed" }
}