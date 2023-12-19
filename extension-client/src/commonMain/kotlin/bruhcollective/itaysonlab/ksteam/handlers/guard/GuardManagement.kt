package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.guard.models.ActiveSession
import bruhcollective.itaysonlab.ksteam.guard.models.AwaitingSession
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import steam.webui.authentication.*

/**
 * Manages Steam Guard instance. (polling auth sessions, disabling, approving)
 */
class GuardManagement(
    private val steamClient: SteamClient
) : BaseHandler {
    /**
     * Creates a Flow which will poll for any new sessions.
     */
    fun createSessionWatcher(): Flow<Long?> {
        return flow {
            while (true) {
                emit(getSessionQueue())
                delay(5000L)
            }
        }
    }

    suspend fun getSessionQueue(): Long? {
        return steamClient.unifiedMessages.execute(
            methodName = "Authentication.GetAuthSessionsForAccount",
            requestAdapter = CAuthentication_GetAuthSessionsForAccount_Request.ADAPTER,
            responseAdapter = CAuthentication_GetAuthSessionsForAccount_Response.ADAPTER,
            requestData = CAuthentication_GetAuthSessionsForAccount_Request()
        ).dataNullable?.client_ids?.firstOrNull()
    }

    suspend fun getActiveSessions(): List<ActiveSession> {
        return steamClient.unifiedMessages.execute(
            methodName = "Authentication.EnumerateTokens",
            requestAdapter = CAuthentication_RefreshToken_Enumerate_Request.ADAPTER,
            responseAdapter = CAuthentication_RefreshToken_Enumerate_Response.ADAPTER,
            requestData = CAuthentication_RefreshToken_Enumerate_Request()
        ).dataNullable?.refresh_tokens?.map(::ActiveSession).orEmpty()
    }

    suspend fun getActiveSessionInfo(id: Long): AwaitingSession? {
        return steamClient.unifiedMessages.execute(
            methodName = "Authentication.GetAuthSessionInfo",
            requestAdapter = CAuthentication_GetAuthSessionInfo_Request.ADAPTER,
            responseAdapter = CAuthentication_GetAuthSessionInfo_Response.ADAPTER,
            requestData = CAuthentication_GetAuthSessionInfo_Request(client_id = id)
        ).dataNullable?.let { AwaitingSession(id, it) }
    }

    // TODO: guess the signature order, current one is not working
    suspend fun revokeSession(id: Long) {
        steamClient.unifiedMessages.execute(
            methodName = "Authentication.RevokeRefreshToken",
            requestAdapter = CAuthentication_RefreshToken_Revoke_Request.ADAPTER,
            responseAdapter = CAuthentication_RefreshToken_Revoke_Response.ADAPTER,
            requestData = CAuthentication_RefreshToken_Revoke_Request(
                token_id = id,
                steamid = steamClient.currentSessionSteamId.longId,
                revoke_action = EAuthTokenRevokeAction.k_EAuthTokenRevokePermanent,
                signature = steamClient.guard.instanceForCurrentUser()!!.sgCreateRevokeSignature(id)
            )
        )
    }

    suspend fun confirmNewSession(session: AwaitingSession, approve: Boolean, persist: Boolean) {
        steamClient.unifiedMessages.execute(
            methodName = "Authentication.UpdateAuthSessionWithMobileConfirmation",
            requestAdapter = CAuthentication_UpdateAuthSessionWithMobileConfirmation_Request.ADAPTER,
            responseAdapter = CAuthentication_UpdateAuthSessionWithMobileConfirmation_Response.ADAPTER,
            requestData = CAuthentication_UpdateAuthSessionWithMobileConfirmation_Request(
                version = session.version,
                client_id = session.id,
                steamid = steamClient.currentSessionSteamId.longId,
                confirm = approve,
                persistence = if (persist) {
                    ESessionPersistence.k_ESessionPersistence_Persistent
                } else {
                    ESessionPersistence.k_ESessionPersistence_Ephemeral
                },
                signature = steamClient.guard.instanceForCurrentUser()!!.sgCreateSignature(session.version, session.id)
            )
        )
    }

    private fun checkForGuard(steamId: SteamId = steamClient.currentSessionSteamId) =
        steamClient.guard.instanceFor(steamId) != null

    override suspend fun onEvent(packet: SteamPacket) = Unit
}