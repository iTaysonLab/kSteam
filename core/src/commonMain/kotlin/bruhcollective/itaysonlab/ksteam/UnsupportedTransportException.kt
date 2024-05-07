package bruhcollective.itaysonlab.ksteam

/**
 * kSteam tried to invoke a method that is not supported by selected [SteamClientConfiguration.TransportMode].
 *
 * For example, this error will be thrown in case a non-RPC method is sent and the transport mode is [SteamClientConfiguration.TransportMode.Web].
 */
class UnsupportedTransportException: Exception(
    "The current kSteam transportation mode does not support this action."
)