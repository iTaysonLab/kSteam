package bruhcollective.itaysonlab.ksteam.guard.clock

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * GuardClockContext is used to correlate the difference between the server and the client.
 *
 * The main usage is "correcting" TOTP codes, so they are the same.
 */
interface GuardClockContext {
    /**
     * Provides a difference between the server time and local machine time
     */
    suspend fun provideTimeDifference(): Long
}

@OptIn(ExperimentalTime::class)
suspend fun GuardClockContext.currentTime() = Clock.System.now().epochSeconds + provideTimeDifference()

@OptIn(ExperimentalTime::class)
suspend fun GuardClockContext.currentTimeMs() = Clock.System.now().toEpochMilliseconds() + (provideTimeDifference() * 1000L)