package bruhcollective.itaysonlab.ksteam.guard.clock

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.storage
import java.util.*

/**
 * A [GuardClockContext] implementation using native Java API.
 *
 * Use it if you are not sure.
 */
class GuardClockContextImpl(
    private val steamClient: SteamClient
) : GuardClockContext {
    private val currentTzId get() = TimeZone.getDefault().id

    override suspend fun provideTimeDifference(): Long {
        if (steamClient.storage.globalConfiguration.clockSyncTz != currentTzId) {
            requestServerDifference(steamClient)
        }

        return steamClient.storage.globalConfiguration.clockSyncDiff
    }

    private suspend fun requestServerDifference(steamClient: SteamClient) {
        steamClient.externalWebApi.getServerTime().let { data ->
            if (data.allowCorrection) {
                data.serverTime - (System.currentTimeMillis() / 1000L)
            } else {
                0L
            }
        }.let { difference ->
            steamClient.storage.modifyConfig {
                copy(clockSyncTz = currentTzId, clockSyncDiff = difference)
            }
        }
    }
}