package bruhcollective.itaysonlab.ksteam.guard.clock

import bruhcollective.itaysonlab.ksteam.SteamClient
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime

/**
 * A [GuardClockContext] implementation using cross-platform KotlinX API.
 *
 * Use it if you are not sure.
 */
class GuardClockContextImpl(
    private val steamClient: SteamClient
) : GuardClockContext {
    private companion object Keys {
        const val KEY_CLOCK_TZ = "clock_timezone"
        const val KEY_CLOCK_DIFF = "clock_diff"
    }

    private val persist get() = steamClient.persistence

    private var clockSyncTz: String
        get() = persist.getString(KEY_CLOCK_TZ).orEmpty()
        set(value) { persist.set(KEY_CLOCK_TZ, value) }

    private var clockSyncDiff: Long
        get() = persist.getLong(KEY_CLOCK_DIFF)
        set(value) { persist.set(KEY_CLOCK_DIFF, value) }

    private val currentTzId get() = TimeZone.currentSystemDefault().id

    override suspend fun provideTimeDifference(): Long {
        if (clockSyncTz != currentTzId) {
            requestServerDifference(steamClient)
        }

        return clockSyncDiff
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun requestServerDifference(steamClient: SteamClient) {
        // TODO: Use Protobuf API
        steamClient.webApi.getServerTime().let { data ->
            if (data.allowCorrection) {
                data.serverTime - Clock.System.now().epochSeconds
            } else {
                0L
            }
        }.let { difference ->
            clockSyncTz = currentTzId
            clockSyncDiff = difference
        }
    }
}