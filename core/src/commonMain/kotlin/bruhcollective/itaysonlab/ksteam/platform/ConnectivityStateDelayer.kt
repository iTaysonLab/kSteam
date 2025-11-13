package bruhcollective.itaysonlab.ksteam.platform

interface ConnectivityStateDelayer {
    /**
     * Suspends until an Internet connection is available.
     */
    suspend fun awaitUntilInternetConnection()

    object Noop : ConnectivityStateDelayer {
        override suspend fun awaitUntilInternetConnection() = Unit
    }
}