package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AuthorizationState
import bruhcollective.itaysonlab.ksteam.platform.*
import com.squareup.wire.ProtoAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import steam.enums.ESessionPersistence
import steam.extra.enums.EResult
import steam.messages.auth.*

class Account(
    private val steamClient: SteamClient
): BaseHandler {
    private val pollScope = CreateSupervisedCoroutineScope("authStatePolling", Dispatchers.Default) { _, _ -> }
    private val deviceInfo get() = steamClient.config.deviceInfo

    // Lazy init because it'll crash on creation
    private val webApi by lazy {
        steamClient.getHandler<WebApi>()
    }

    private var authStateWatcher: Job? = null
    private var authState = MutableStateFlow<AuthorizationState>(AuthorizationState.Unauthorized)
    val clientAuthState = authState.asStateFlow()

    /**
     * Gets all necessary data to generate a sign-in QR code which can be scanned from the mobile app or other kSteam instance.
     *
     * Also starts a polling session.
     */
    suspend fun getSignInQrCode(): QrCodeData? {
        val qrData = webApi.execute(
            signed = false,
            methodName = "Authentication.BeginAuthSessionViaQR",
            requestAdapter = CAuthentication_BeginAuthSessionViaQR_Request.ADAPTER,
            responseAdapter = CAuthentication_BeginAuthSessionViaQR_Response.ADAPTER,
            requestData = CAuthentication_BeginAuthSessionViaQR_Request(
                device_friendly_name = deviceInfo.deviceName,
                device_details = deviceInfo.toAuthDetails(),
                platform_type = deviceInfo.platformType,
                website_id = "Unknown"
            )
        )

        return if (qrData.isSuccess) {
            with(qrData.data) {
                createWatcherFlow(this.interval ?: 5f, this.client_id!!, this.request_id!!)
                QrCodeData((this.version ?: 0) to this.challenge_url.orEmpty())
            }
        } else {
            null
        }
    }

    /**
     * Signs in using a username and a password.
     */
    suspend fun signIn(
        username: String,
        password: String,
        rememberSession: Boolean = true,
    ): AuthorizationResult {
        val rsaData = webApi.execute(
            signed = false,
            methodName = "Authentication.GetPasswordRSAPublicKey",
            requestAdapter = CAuthentication_GetPasswordRSAPublicKey_Request.ADAPTER,
            responseAdapter = CAuthentication_GetPasswordRSAPublicKey_Response.ADAPTER,
            requestData = CAuthentication_GetPasswordRSAPublicKey_Request(account_name = username)
        ).data

        val encryptedPassword = Cryptography.rsaEncrypt(password, rsaData.publickey_mod.orEmpty(), rsaData.publickey_exp.orEmpty()).toByteString().base64().dropLast(1)

        val signInResult = webApi.execute(
            signed = false,
            methodName = "Authentication.BeginAuthSessionViaCredentials",
            requestAdapter = CAuthentication_BeginAuthSessionViaCredentials_Request.ADAPTER,
            responseAdapter = CAuthentication_BeginAuthSessionViaCredentials_Response.ADAPTER,
            requestData = CAuthentication_BeginAuthSessionViaCredentials_Request(
                device_friendly_name = deviceInfo.deviceName,
                device_details = deviceInfo.toAuthDetails(),
                platform_type = deviceInfo.platformType,
                website_id = "Unknown",
                remember_login = rememberSession,
                persistence = if (rememberSession) ESessionPersistence.k_ESessionPersistence_Persistent else ESessionPersistence.k_ESessionPersistence_Ephemeral,
                account_name = username,
                encrypted_password = encryptedPassword,
                encryption_timestamp = rsaData.timestamp
            )
        )

        return if (signInResult.result == EResult.InvalidPassword) {
            AuthorizationResult.InvalidPassword
        } else {
            with(signInResult.data) {
                authState.tryEmit(AuthorizationState.AwaitingTwoFactor(this))
                createWatcherFlow(this.interval ?: 5f, this.client_id!!, this.request_id!!)
            }

            AuthorizationResult.Success
        }
    }

    /**
     * Tries to sign in using saved session data from [Storage].
     * You can specify your own SteamID to sign in a particular account. If this parameter is null, the "default" SteamID will be selected.
     */
    suspend fun trySignInSaved(
        steamId: Long? = null
    ) {

    }

    private suspend fun pollAuthStatus(clientId: Long, requestId: ByteString): CAuthentication_PollAuthSessionStatus_Response {
        return webApi.execute(
            signed = false,
            methodName = "Authentication.PollAuthSessionStatus",
            requestAdapter = CAuthentication_PollAuthSessionStatus_Request.ADAPTER,
            responseAdapter = CAuthentication_PollAuthSessionStatus_Response.ADAPTER,
            requestData = CAuthentication_PollAuthSessionStatus_Request(client_id = clientId, request_id = requestId)
        ).data
    }

    private fun createWatcherFlow(interval: Float, clientId: Long, requestId: ByteString) {
        authStateWatcher?.cancel()

        authStateWatcher = flow<Unit> {
            while (true) {
                val pollAnswer = pollAuthStatus(clientId, requestId)

                if (pollAnswer.access_token != null && pollAnswer.refresh_token != null) {
                    // Success, now we can cancel the session
                    break
                } else {
                    delay((interval * 1000).toLong())
                }
            }
        }.onCompletion {
            authStateWatcher = null
        }.launchIn(pollScope)
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            else -> {}
        }
    }

    enum class AuthorizationResult {
        // Now you should use the state from Account.accountAuthState to show TFA interface
        Success,
        // The password does not match.
        InvalidPassword
    }

    @JvmInline
    value class QrCodeData(private val packed: Pair<Int, String>) {
        val version get() = packed.first
        val data get() = packed.second
    }
}