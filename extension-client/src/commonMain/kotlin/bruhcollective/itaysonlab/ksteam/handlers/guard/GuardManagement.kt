package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.guard.models.ActiveSession
import bruhcollective.itaysonlab.ksteam.guard.models.IncomingSession
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import steam.enums.EAuthTokenRevokeAction
import steam.enums.ESessionPersistence
import steam.webui.authentication.*

/**
 * Manages Steam Guard instance: revoking active sessions, approving or rejecting incoming sessions
 */
class GuardManagement(
    private val steamClient: ExtendedSteamClient
) {
    /**
     * Creates a Flow which will poll *every 5 seconds* for any new sessions that needs to be confirmed.
     *
     * `null` means that there are no sessions waiting to be approved.
     */
    fun createIncomingSessionWatcher(): Flow<Long?> {
        return flow {
            while (currentCoroutineContext().isActive) {
                emit(getIncomingSessionIdQueue().firstOrNull())
                delay(5000L)
            }
        }
    }

    /**
     * Returns the list of sign-in session IDs that are awaiting to be confirmed.
     */
    suspend fun getIncomingSessionIdQueue(): List<Long> {
        return steamClient.grpc.authentication.GetAuthSessionsForAccount().executeSteam(
            data = CAuthentication_GetAuthSessionsForAccount_Request()
        ).client_ids
    }

    /**
     * Returns currently approved sessions for the account.
     */
    suspend fun getActiveSessions(): ActiveSessions {
        return steamClient.grpc.authentication.EnumerateTokens().executeSteam(
            data = CAuthentication_RefreshToken_Enumerate_Request()
        ).let { response ->
            response.refresh_tokens.partition { refreshToken -> refreshToken.token_id == response.requesting_token }.let { parted ->
                ActiveSessions(
                    currentSession = ActiveSession(parted.first.first(), true),
                    sessions = parted.second.map(::ActiveSession)
                )
            }
        }
    }

    /**
     * Returns an awaiting session, mostly used when fetching authorization data from a QR code or [createSessionWatcher].
     */
    suspend fun getIncomingSessionInfo(id: Long): IncomingSession? {
        return runCatching {
            steamClient.grpc.authentication.GetAuthSessionInfo().executeSteam(
                data = CAuthentication_GetAuthSessionInfo_Request(client_id = id)
            ).let { IncomingSession(id, it) }
        }.getOrNull()
    }

    /**
     * Revokes a specific session. Requires Steam Guard to be initialized in this kSteam instance.
     */
    suspend fun revokeSession(id: Long) {
        steamClient.grpc.authentication.RevokeRefreshToken().executeSteam(
            web = true,
            data = CAuthentication_RefreshToken_Revoke_Request(
                token_id = id,
                steamid = steamClient.currentSessionSteamId.longId,
                revoke_action = EAuthTokenRevokeAction.k_EAuthTokenRevokePermanent.ordinal,
                signature = steamClient.guard.instanceForCurrentUser()?.sgCreateRevokeSignature(id) ?: error("To revoke sessions, kSteam must have Steam Guard data.")
            )
        )
    }

    /**
     * Revokes this session. Unlike [revokeSession], does not require Steam Guard data.
     */
    suspend fun revokeCurrentSession() {
        steamClient.grpc.authentication.RevokeRefreshToken().executeSteam(
            web = true,
            data = CAuthentication_RefreshToken_Revoke_Request(
                revoke_action = EAuthTokenRevokeAction.k_EAuthTokenRevokeLogout.ordinal
            )
        )
    }

    /**
     * Approves an incoming session.
     *
     * @param session an incoming session
     * @param persist if an incoming session should be longer ("Remember password for this client")
     */
    suspend fun approveIncomingSession(session: IncomingSession, persist: Boolean) {
        val persistence = if (persist) {
            ESessionPersistence.k_ESessionPersistence_Persistent
        } else {
            ESessionPersistence.k_ESessionPersistence_Ephemeral
        }

        steamClient.grpc.authentication.UpdateAuthSessionWithMobileConfirmation().executeSteam(
            data = CAuthentication_UpdateAuthSessionWithMobileConfirmation_Request(
                version = session.version,
                client_id = session.id,
                steamid = steamClient.currentSessionSteamId.longId,
                confirm = true,
                persistence = persistence.ordinal,
                signature = steamClient.guard.instanceForCurrentUser()?.sgCreateSignature(session.version, session.id) ?: error("To approve sessions, kSteam must have Steam Guard data.")
            )
        )
    }

    /**
     * Rejects an incoming session.
     *
     * @param session an incoming session
     */
    suspend fun rejectIncomingSession(session: IncomingSession) {
        steamClient.grpc.authentication.UpdateAuthSessionWithMobileConfirmation().executeSteam(
            data = CAuthentication_UpdateAuthSessionWithMobileConfirmation_Request(
                version = session.version,
                client_id = session.id,
                steamid = steamClient.currentSessionSteamId.longId,
                confirm = false,
                persistence = ESessionPersistence.k_ESessionPersistence_Ephemeral.ordinal,
                signature = steamClient.guard.instanceForCurrentUser()?.sgCreateSignature(session.version, session.id) ?: error("To reject sessions, kSteam must have Steam Guard data.")
            )
        )
    }

    /**
     * Represents active sessions that were approved to access a Steam account.
     *
     * @param currentSession the current session from which [getActiveSessions] was called
     * @param sessions other sessions that got access to the account
     */
    data class ActiveSessions (
        val currentSession: ActiveSession,
        val sessions: List<ActiveSession>
    )
}