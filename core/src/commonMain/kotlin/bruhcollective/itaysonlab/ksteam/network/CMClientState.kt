package bruhcollective.itaysonlab.ksteam.network

/**
 * Defines the connection state of the kSteam client.
 */
enum class CMClientState {
    /**
     * kSteam instance is created, but it is in "offline" mode - no connection attempted.
     *
     * Use [bruhcollective.itaysonlab.ksteam.SteamClient.start] to move to the [Connecting] state.
     */
    Offline,

    /**
     * kSteam is trying to connect to the Steam network.
     */
    Connecting,

    /**
     * kSteam encountered a connection issue and is trying to reconnect.
     */
    Reconnecting,

    /**
     * kSteam is connected to the Steam network as a guest. Limited functionality is available.
     */
    AwaitingAuthorization,

    /**
     * kSteam successfully connected to the Steam network and is trying to sign in with the provided credentials
     */
    Authorizing,

    /**
     * kSteam successfully signed in to the Steam network and ready to accept requests
     */
    Connected,

    /**
     * An error occurred while connecting to the Steam network.
     */
    Error;

    val hasActiveServerConnection: Boolean get() = this == AwaitingAuthorization || this == Authorizing || this == Connected
}