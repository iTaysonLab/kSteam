package bruhcollective.itaysonlab.ksteam.guard.clock

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

suspend fun GuardClockContext.currentTime() = (System.currentTimeMillis() / 1000L) + provideTimeDifference()
suspend fun GuardClockContext.currentTimeMs() = System.currentTimeMillis() + (provideTimeDifference() * 1000L)