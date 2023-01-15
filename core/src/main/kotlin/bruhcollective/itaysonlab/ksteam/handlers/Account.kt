package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.debug.logWarning
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AuthorizationState
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.platform.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.platform.Cryptography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import steam.enums.ESessionPersistence
import steam.messages.auth.*
import steam.messages.clientserver_login.CMsgClientLogon
import steam.messages.clientserver_login.CMsgClientLogonResponse
import kotlin.random.Random

class Account(
    private val steamClient: SteamClient
) : BaseHandler {
    private val pollScope = CreateSupervisedCoroutineScope("authStatePolling", Dispatchers.Default) { _, _ -> }
    private val deviceInfo get() = steamClient.config.deviceInfo

    private val webApi get() = steamClient.getHandler<WebApi>()
    private val storage get() = steamClient.getHandler<Storage>()

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

        val encryptedPassword =
            Cryptography.rsaEncrypt(password, rsaData.publickey_mod.orEmpty(), rsaData.publickey_exp.orEmpty())
                .toByteString().base64().dropLast(1)

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
                logVerbose(
                    "Account:SignIn",
                    "Success, waiting for 2FA. Available confirmations: ${this.allowed_confirmations.joinToString()}"
                )

                authState.tryEmit(AuthorizationState.AwaitingTwoFactor(this))
                createWatcherFlow(this.interval ?: 5f, this.client_id!!, this.request_id!!)
            }

            AuthorizationResult.Success
        }
    }

    /**
     * Update a current session with a Steam Guard or email code.
     */
    suspend fun updateCurrentSessionWithCode(code: String) {
        require(clientAuthState.value is AuthorizationState.AwaitingTwoFactor) { "Current session does not want to receive 2FA codes" }

        (clientAuthState.value as AuthorizationState.AwaitingTwoFactor).let { authState ->
            webApi.execute(
                signed = false,
                methodName = "Authentication.UpdateAuthSessionWithSteamGuardCode",
                requestAdapter = CAuthentication_UpdateAuthSessionWithSteamGuardCode_Request.ADAPTER,
                responseAdapter = CAuthentication_UpdateAuthSessionWithSteamGuardCode_Response.ADAPTER,
                requestData = CAuthentication_UpdateAuthSessionWithSteamGuardCode_Request(
                    client_id = authState.clientId,
                    steamid = authState.steamId.id.toLong(),
                    code = code,
                    code_type = authState.sumProtos.first()
                )
            )
        }
    }

    /**
     * Tries to sign in using saved session data from [Storage].
     * You can specify your own SteamID to sign in a particular account. If this parameter is null, the "default" SteamID will be selected.
     *
     * @return if there is an account available
     */
    suspend fun trySignInSaved(
        steamId: SteamId? = null
    ): Boolean {
        val accountToSignIn = if (steamId != null) {
            storage.globalConfiguration.availableAccounts[steamId.id]
        } else {
            storage.globalConfiguration.availableAccounts[storage.globalConfiguration.defaultAccount]
        } ?: storage.globalConfiguration.availableAccounts.values.firstOrNull()

        return if (accountToSignIn != null) {
            sendClientLogon(steamId = SteamId(accountToSignIn.steamId), token = accountToSignIn.refreshToken)
            true
        } else {
            if (steamId != null) {
                logWarning("Account:AutoSignIn", "No accounts found on the kSteam database. Please log in manually to use this feature.")
            }

            false
        }
    }

    private suspend fun sendClientLogon(token: String, steamId: SteamId) {
        steamClient.executeAndForget(SteamPacket.newProto(
            EMsg.k_EMsgClientLogon, CMsgClientLogon.ADAPTER, CMsgClientLogon(
                protocol_version = EnvironmentConstants.PROTOCOL_VERSION,
                client_package_version = 1671236931,
                client_language = "english",
                client_os_type = steamClient.config.deviceInfo.osType.encoded,
                should_remember_password = true,
                qos_level = 2,
                machine_id = Random.nextBytes(64).toByteString(),
                machine_name = steamClient.config.deviceInfo.deviceName,
                eresult_sentryfile = EResult.Fail.encoded,
                steamguard_dont_remember_computer = false,
                is_steam_deck = false,
                is_steam_box = false,
                client_instance_id = 0L,
                supports_rate_limit_response = true,
                access_token = token,
                sha_sentryfile = null,
            )
        ).withHeader {
            this.sessionId = 0
            this.steamId = steamId.id
        })
    }

    /**
     * Cancel authentication polling, if you have used [getSignInQrCode].
     */
    suspend fun cancelPolling() = authStateWatcher?.cancel()

    suspend fun awaitSignIn() = clientAuthState.first { it is AuthorizationState.Success }

    private suspend fun pollAuthStatus(
        clientId: Long,
        requestId: ByteString
    ): CAuthentication_PollAuthSessionStatus_Response {
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
                    logVerbose("Account:Watcher", "Succesfully logged in: $pollAnswer")

                    val steamId = (clientAuthState.value as AuthorizationState.AwaitingTwoFactor).steamId

                    storage.modifyAccount(steamId) {
                        copy(
                            accessToken = pollAnswer.access_token.orEmpty(),
                            refreshToken = pollAnswer.refresh_token.orEmpty(),
                            accountName = pollAnswer.account_name.orEmpty()
                        )
                    }

                    sendClientLogon(
                        token = pollAnswer.refresh_token.orEmpty(),
                        steamId = steamId
                    )

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
            EMsg.k_EMsgClientLogOnResponse -> {
                if (packet.isProtobuf()) {
                    val response = packet.getProtoPayload(CMsgClientLogonResponse.ADAPTER)

                    if (response.data.eresult == EResult.OK.encoded) {
                        // We are logged in to Steam3 server
                        authState.value = AuthorizationState.Success
                    }
                } else {
                    // Ignore, that's kicked out for inactivity, Restarter should reconnect again
                }
            }

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