package bruhcollective.itaysonlab.ksteam.platform

import kotlin.time.Duration

interface ConnectivityStateDelayer {
    /**
     * Suspends until an Internet connection is available.
     */
    suspend fun awaitUntilInternetConnection(timeout: Duration? = null)

    object Noop : ConnectivityStateDelayer {
        override suspend fun awaitUntilInternetConnection(timeout: Duration?) = Unit
    }
}