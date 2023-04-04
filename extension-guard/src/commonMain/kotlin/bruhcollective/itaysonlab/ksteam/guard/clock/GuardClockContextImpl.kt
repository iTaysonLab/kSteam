package bruhcollective.itaysonlab.ksteam.guard.clock

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.guard.models.ClockSyncConfiguration
import bruhcollective.itaysonlab.ksteam.handlers.storage
import bruhcollective.itaysonlab.ksteam.platform.fileProxiedObject
import bruhcollective.itaysonlab.ksteam.platform.getValue
import bruhcollective.itaysonlab.ksteam.platform.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

/**
 * A [GuardClockContext] implementation using native Java API.
 *
 * Use it if you are not sure.
 */
class GuardClockContextImpl(
    private val steamClient: SteamClient
) : GuardClockContext {
    private var configuration by fileProxiedObject(steamClient.storage.rootFolder / "guardClockSync.json", ClockSyncConfiguration())

    private val currentTzId get() = TimeZone.currentSystemDefault().id

    override suspend fun provideTimeDifference(): Long {
        if (configuration.clockSyncTz != currentTzId) {
            requestServerDifference(steamClient)
        }

        return configuration.clockSyncDiff
    }

    private suspend fun requestServerDifference(steamClient: SteamClient) {
        steamClient.webApi.getServerTime().let { data ->
            if (data.allowCorrection) {
                data.serverTime - Clock.System.now().epochSeconds
            } else {
                0L
            }
        }.let { difference ->
            configuration = configuration.copy(
                clockSyncTz = currentTzId,
                clockSyncDiff = difference
            )
        }
    }
}