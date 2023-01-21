package bruhcollective.itaysonlab.ksteam.guard.clock

import bruhcollective.itaysonlab.ksteam.SteamClient

/**
 * A [GuardClockContext] implementation which does not query the server.
 */
class NoopGuardClockContextImpl: GuardClockContext {
    override suspend fun provideTimeDifference() = 0L
}