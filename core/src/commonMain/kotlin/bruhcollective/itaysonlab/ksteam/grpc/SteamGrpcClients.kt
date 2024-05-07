package bruhcollective.itaysonlab.ksteam.grpc

import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import steam.webui.authentication.AuthenticationClient
import steam.webui.authentication.GrpcAuthenticationClient

interface SteamGrpcClients {
    val authenticationClient: AuthenticationClient
}

internal class SteamGrpcClientsImpl (
    unifiedMessages: UnifiedMessages
): SteamGrpcClients {
    override val authenticationClient: AuthenticationClient = GrpcAuthenticationClient(unifiedMessages)
}