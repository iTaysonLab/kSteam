package bruhcollective.itaysonlab.ksteam.guard.clock

import kotlinx.datetime.Clock

/**
 * GuardClockContext is used to correlate the difference between the server and the client.
 *
 * The main usage is "correcting" TOTP codes so they are the same.
 */
interface GuardClockContext {
    /**
     * Provides a difference between the server time and local machine time
     */
    suspend fun provideTimeDifference(): Long
}

suspend fun GuardClockContext.currentTime() = Clock.System.now().epochSeconds + provideTimeDifference()
suspend fun GuardClockContext.currentTimeMs() = Clock.System.now().toEpochMilliseconds() + (provideTimeDifference() * 1000L)