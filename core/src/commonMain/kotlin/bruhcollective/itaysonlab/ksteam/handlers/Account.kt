package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.AuthPrivateIpLogic
import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.internal.Sentry
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.account.AuthorizationState
import bruhcollective.itaysonlab.ksteam.models.account.SteamAccountAuthorization
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.network.CMClientState
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobRemoteException
import bruhcollective.itaysonlab.ksteam.platform.encryptWithRsa
import bruhcollective.itaysonlab.ksteam.platform.getIpv4Address
import bruhcollective.itaysonlab.ksteam.util.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import steam.enums.EAuthSessionGuardType
import steam.enums.EAuthTokenPlatformType
import steam.enums.ESessionPersistence
import steam.webui.authentication.*
import steam.webui.common.CMsgClientLogon
import steam.webui.common.CMsgClientLogonResponse
import kotlin.jvm.JvmInline
import kotlin.random.Random

class Account internal constructor(
    private val steamClient: SteamClient
) {
    private val sentry: Sentry = Sentry(steamClient)

    private companion object {
        const val TAG = "Account"
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val pollScope = CreateSupervisedCoroutineScope("authStatePolling", Dispatchers.Default) { _, _ -> }
    private val deviceInfo get() = steamClient.deviceInfo

    private var pollInfo: PollInfo? = null
    private var authStateWatcher: Job? = null
    private var authState = MutableStateFlow<AuthorizationState>(AuthorizationState.Unauthorized)
    val clientAuthState = authState.asStateFlow()

    internal var tokenRequested = MutableStateFlow(false)

    private var logonSteamId: SteamId = SteamId.Empty
    private var onLogonAttemptCallbacks = mutableListOf<LogonAttemptListener>()

    fun getSignAttemptedSteamId(): SteamId = logonSteamId

    /**
     * Gets all necessary data to generate a sign-in QR code which can be scanned from the mobile app or other kSteam instance.
     *
     * Also starts a polling session.
     */
    suspend fun getSignInQrCode(): QrCodeData? {
        val qrData = try {
            steamClient.grpc.authentication.BeginAuthSessionViaQR().executeSteam(
                CAuthentication_BeginAuthSessionViaQR_Request(
                    device_friendly_name = deviceInfo.deviceName,
                    device_details = deviceInfo.toAuthDetails(),
                    platform_type = deviceInfo.platformType.ordinal,
                    website_id = if (deviceInfo.platformType == EAuthTokenPlatformType.k_EAuthTokenPlatformType_MobileApp) {
                        "Mobile"
                    } else {
                        "Unknown"
                    }
                ), anonymous = true
            )
        } catch (e: CMJobRemoteException) {
            return null
        }

        pollInfo = PollInfo(qrData.client_id!! to qrData.request_id!!)
        createWatcherFlow(qrData.interval ?: 5f)

        return QrCodeData((qrData.version ?: 0) to qrData.challenge_url.orEmpty())
    }

    /**
     * Signs in using a username and a password.
     *
     * @param username username
     * @param password password
     * @param rememberSession if core-persistence should be invoked (if installed)
     *
     * @return [AuthorizationResult] with further steps
     */
    suspend fun signIn(
        username: String,
        password: String,
        rememberSession: Boolean = true,
    ): AuthorizationResult {
        val rsaData = steamClient.grpc.authentication.GetPasswordRSAPublicKey().executeSteam(
            CAuthentication_GetPasswordRSAPublicKey_Request(account_name = username),
            anonymous = true
        )

        val encryptedPassword =
            encryptWithRsa(password, rsaData.publickey_mod.orEmpty(), rsaData.publickey_exp.orEmpty())
                .toByteString().base64().dropLast(1)

        val signInResult = try {
            steamClient.grpc.authentication.BeginAuthSessionViaCredentials().executeSteam(
                CAuthentication_BeginAuthSessionViaCredentials_Request(
                    device_friendly_name = deviceInfo.deviceName,
                    device_details = deviceInfo.toAuthDetails(),
                    platform_type = deviceInfo.platformType.ordinal,
                    website_id = if (deviceInfo.platformType == EAuthTokenPlatformType.k_EAuthTokenPlatformType_MobileApp) {
                        "Mobile"
                    } else {
                        "Unknown"
                    },
                    remember_login = rememberSession,
                    persistence = (if (rememberSession) ESessionPersistence.k_ESessionPersistence_Persistent else ESessionPersistence.k_ESessionPersistence_Ephemeral).value,
                    account_name = username,
                    encrypted_password = encryptedPassword,
                    encryption_timestamp = rsaData.timestamp,
                ), anonymous = true
            )
        } catch (e: CMJobRemoteException) {
            return if (e.result == EResult.InvalidPassword) {
                AuthorizationResult.InvalidPassword
            } else {
                AuthorizationResult.RpcError
            }
        }

        steamClient.logger.logVerbose(TAG) { "[login] success, waiting for 2FA with available types: [${signInResult.allowed_confirmations.joinToString()}]" }

        pollInfo = PollInfo(signInResult.client_id!! to signInResult.request_id!!)
        authState.emit(AuthorizationState.AwaitingTwoFactor(signInResult))

        val mappedConfirmations = signInResult.allowed_confirmations.mapNotNull { EAuthSessionGuardType.fromValue(it.confirmation_type ?: 0) }

        if (mappedConfirmations.let {
            it.contains(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation) || it.contains(EAuthSessionGuardType.k_EAuthSessionGuardType_EmailConfirmation)
        }) {
            createWatcherFlow(signInResult.interval ?: 5f)
        }

        /*if (mappedConfirmations.contains(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode)) {
            val steamId = SteamId(signInResult.steamid?.toULong() ?: 0u)
            var guardCode: String? = null

            for (handler in steamClient.getImplementingHandlers<SteamGuardPlugin>()) {
                val hCode = handler.getCodeFor(steamId)

                if (hCode != null) {
                    guardCode = hCode
                    break
                }
            }

            if (guardCode != null) {
                steamClient.logger.logVerbose(TAG) { "[login] kSteam persistence has Steam Guard for this SteamID, skipping 2FA..." }
                updateCurrentSessionWithCode(guardCode)
            }
        }*/

        return AuthorizationResult.ProceedToTfa
    }

    /**
     * Update a current session with a Steam Guard or email code.
     *
     * @param code 2FA code
     * @return if 2FA code is valid
     */
    suspend fun updateCurrentSessionWithCode(code: String): Boolean {
        require(clientAuthState.value is AuthorizationState.AwaitingTwoFactor) { "Current session does not want to receive 2FA codes" }

        (clientAuthState.value as AuthorizationState.AwaitingTwoFactor).let { authState ->
            steamClient.grpc.authentication.UpdateAuthSessionWithSteamGuardCode().executeSteam(
                CAuthentication_UpdateAuthSessionWithSteamGuardCode_Request(
                    client_id = pollInfo!!.clientId,
                    steamid = authState.steamId.longId,
                    code = code,
                    code_type = authState.sumProtos.filterNot {
                        it == EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation || it == EAuthSessionGuardType.k_EAuthSessionGuardType_EmailConfirmation
                    }.first().value
                ), anonymous = true
            )
        }

        return pollAuthStatusInternal()
    }

    /**
     * Sign in with already acquired Steam account **refresh** token.
     *
     * Note that depending on the device type used when obtaining the token you may encounter different issues, such as "Access denied" errors on some API endpoints.
     *
     * @param steamId SteamID of the requested account, must not be 0
     * @param refreshToken refresh token of the Steam account
     */
    suspend fun signInWithRefreshToken(
        steamId: SteamId,
        refreshToken: String,
    ) {
        require(steamId.id != SteamId.Empty.id) { "SteamID should not be 0 in order to sign in with an access token." }
        sendClientLogon(steamId = steamId, token = refreshToken)
    }

    /**
     * Tries to sign in using saved session data from [Configuration]. Requires `core-persistence` module connected.
     *
     * @return if there is an account available
     */
    suspend fun trySignInSaved(steamId: SteamId): Boolean {
        val autoSteamAuthorization = steamClient.configuration.getSecureAccount(steamId)

        if (autoSteamAuthorization != null) {
            sendClientLogon(steamId = steamId, token = autoSteamAuthorization.refreshToken)
            return true
        } else {
            steamClient.logger.logWarning(TAG) { "[autologin] ID $steamId requested but was not found, skipping..." }
            return false
        }
    }

    /**
     * Tries to sign in using saved session data from [Configuration]. Requires `core-persistence` module connected.
     * Unlike [trySignInSaved], this will use the default SteamID specified in settings, or the first available one.
     *
     * @return if there is an account available
     */
    suspend fun trySignInSavedDefault(): Boolean {
        val autoSteamAuthorization = steamClient.configuration.getSecureAccount(steamClient.configuration.autologinSteamId)

        val accountToUse = if (autoSteamAuthorization != null) {
            // Default SteamID selected
            autoSteamAuthorization
        } else {
            // Default SteamID was not found - trying next one...
            steamClient.logger.logDebug(TAG) { "[autologin] default steamid was not found, trying the first available..." }

            val nextSteamId = steamClient.configuration.getValidSecureAccountIds().firstOrNull() ?: SteamId.Empty
            steamClient.configuration.autologinSteamId = nextSteamId
            steamClient.configuration.getSecureAccount(nextSteamId)
        }

        if (accountToUse != null) {
            sendClientLogon(steamId = steamClient.configuration.autologinSteamId, token = accountToUse.refreshToken)
            return true
        } else {
            steamClient.logger.logWarning(TAG) { "[autologin] no saved accounts in kSteam persistence, skipping..." }
            return false
        }
    }

    private suspend fun sendClientLogon(token: String, steamId: SteamId) {
        logonSteamId = steamId
        onLogonAttemptCallbacks.forEach { it.onAttempt(steamId) }

        if (steamClient.configuration.machineId.isEmpty()) {
            steamClient.configuration.machineId = Random.nextBytes(64).toByteString().hex()
        }

        val sentryFileHash = sentry.sentryHash(steamId)

        val currentIp = when (steamClient.authPrivateIpLogic) {
            AuthPrivateIpLogic.UsePrivateIp -> {
                convertToCmIpV4(getIpv4Address())
            }

            AuthPrivateIpLogic.Generate -> {
                convertToCmIpV4(generateIpV4Int())
            }

            AuthPrivateIpLogic.None -> null
        }

        steamClient.execute(SteamPacket.newProto(
            EMsg.k_EMsgClientLogon, CMsgClientLogon(
                protocol_version = EnvironmentConstants.PROTOCOL_VERSION,
                client_package_version = 1671236931,
                client_language = steamClient.language.vdfName,
                client_os_type = deviceInfo.osType.encoded,
                should_remember_password = true,
                qos_level = 2,
                machine_id = steamClient.configuration.machineId.decodeHex(),
                machine_name = deviceInfo.deviceName,
                obfuscated_private_ip = currentIp,
                deprecated_obfustucated_private_ip = currentIp?.v4,
                steamguard_dont_remember_computer = false,
                is_steam_deck = false,
                is_steam_box = false,
                client_instance_id = 0L,
                supports_rate_limit_response = true,
                access_token = token,
                eresult_sentryfile = if (sentryFileHash != null) {
                    EResult.OK.encoded
                } else {
                    EResult.Fail.encoded
                },
                sha_sentryfile = sentryFileHash,
            )
        ).withHeader {
            this.sessionId = 0
            this.steamId = steamId.id
        })
    }

    /**
     * Cancels sign in attempt. Reset states and cancels polling.
     */
    fun cancelSignInAttempt() {
        authState.value = AuthorizationState.Unauthorized
        cancelPolling()
    }

    /**
     * Cancel authentication polling, if you have used [getSignInQrCode].
     */
    fun cancelPolling() = authStateWatcher?.cancel()

    suspend fun awaitSignIn() = clientAuthState.first { it is AuthorizationState.Success }

    fun hasSavedDataForAtLeastOneAccount(): Boolean {
        return steamClient.configuration.getValidSecureAccountIds().isNotEmpty()
    }

    private suspend fun pollAuthStatus(): CAuthentication_PollAuthSessionStatus_Response {
        require(pollInfo != null) { "pollInfo should not be null" }

        return steamClient.grpc.authentication.PollAuthSessionStatus().executeSteam(
            CAuthentication_PollAuthSessionStatus_Request(
                client_id = pollInfo!!.clientId,
                request_id = pollInfo!!.requestId
            ), anonymous = true
        )
    }

    private fun createWatcherFlow(interval: Float) {
        authStateWatcher?.cancel()
        authStateWatcher = flow<Unit> {
            while (currentCoroutineContext().isActive) {
                if (pollAuthStatusInternal()) {
                    break
                } else {
                    delay((interval * 1000).toLong())
                }
            }
        }.onCompletion {
            authStateWatcher = null
        }.launchIn(pollScope)
    }

    private suspend fun pollAuthStatusInternal(): Boolean {
        val pollAnswer = pollAuthStatus()

        return if (pollAnswer.access_token != null && pollAnswer.refresh_token != null) {
            // Success, now we can cancel the session
            steamClient.logger.logVerbose(TAG) { "[poller] successfully logged in -> $pollAnswer" }

            val steamId = try {
                SteamId(json.decodeFromString<JwtToken>(pollAnswer.refresh_token!!.split(".")[1].decodeBase64String()).sub.toULong())
            } catch (e: Exception) {
                e.printStackTrace()
                (clientAuthState.value as? AuthorizationState.AwaitingTwoFactor)?.steamId
            }

            if (steamId == null) {
                steamClient.logger.logError(TAG) { "[poller] received JWT, but no Steam ID exposed: couldn't continue signing in" }
                return true
            }

            steamClient.configuration.autologinSteamId = steamId
            steamClient.configuration.updateSecureAccount(steamId, SteamAccountAuthorization(
                accessToken = pollAnswer.access_token!!,
                refreshToken = pollAnswer.refresh_token!!,
                accountName = pollAnswer.account_name.orEmpty()
            ))

            sendClientLogon(
                token = pollAnswer.refresh_token!!,
                steamId = steamId
            )

            true
        } else {
            false
        }
    }

    init {
        steamClient.onClientState(CMClientState.AwaitingAuthorization) {
            trySignInSavedDefault()
        }

        steamClient.on(EMsg.k_EMsgClientLogOnResponse) { packet ->
            if (packet.success && packet.isProtobuf()) {
                CMsgClientLogonResponse.ADAPTER.decode(packet.payload).also { response ->
                    steamClient.logger.logVerbose(TAG) { "Logon Response: $response" }

                    if (response.eresult == EResult.Expired.encoded) {
                        // This authorization session is gone - we can wipe the regarding data
                        steamClient.configuration.deleteSecureAccount(logonSteamId)
                        steamClient.configuration.autologinSteamId = steamClient.configuration.getValidSecureAccountIds().firstOrNull() ?: SteamId.Empty
                        return@on
                    } else if (response.eresult != EResult.OK.encoded) {
                        return@on
                    }

                    steamClient.configuration.cellId = response.cell_id ?: 0
                }

                // We are logged in to Steam3 server
                authStateWatcher?.cancel()
                authState.value = AuthorizationState.Success

                updateAccessToken()
                tokenRequested.value = true
            } else {
                // Ignore, that's kicked out for inactivity, Restarter should reconnect again
            }
        }

        steamClient.on(EMsg.k_EMsgClientLoggedOff) {
            tokenRequested.value = false
        }
    }

    fun getDefaultAccount() = steamClient.configuration.getSecureAccount(steamClient.configuration.autologinSteamId)
    fun getCurrentAccount() = steamClient.configuration.getSecureAccount(steamClient.currentSessionSteamId)
    fun buildSteamLoginSecureCookie() = getCurrentAccount()?.let { "${steamClient.currentSessionSteamId}||${it.accessToken}" }.orEmpty()

    suspend fun awaitTokenRequested() {
        tokenRequested.first { it }
    }

    /**
     * Returns a list of cookies to be used for AJAX requests or WebView
     */
    fun getWebCookies(): List<Pair<String, String>> {
        return listOf(
            "mobileClient" to "android",
            "mobileClientVersion" to "777777 3.7.4",
            "steamLoginSecure" to buildSteamLoginSecureCookie()
        )
    }

    suspend fun awaitWebCookies(): List<Pair<String, String>> {
        awaitTokenRequested()
        return getWebCookies()
    }

    suspend fun updateAccessToken() {
        try {
            getCurrentAccount()?.let { account ->
                    steamClient.grpc.authentication.GenerateAccessTokenForApp().executeSteam(
                    CAuthentication_AccessToken_GenerateForApp_Request(
                        refresh_token = account.refreshToken,
                        steamid = steamClient.currentSessionSteamId.longId
                    )
                ).access_token?.let { newAccessToken ->
                    steamClient.configuration.updateSecureAccount(steamClient.currentSessionSteamId, account.copy(
                        accessToken = newAccessToken
                    ))
                }
            }
        } catch (e: CMJobRemoteException) {
            //
        }
    }

    enum class AuthorizationResult {
        // Now you should use the state from Account.clientAuthState to show TFA interface
        ProceedToTfa,

        // The password does not match.
        InvalidPassword,

        // RPC error has occurred
        RpcError
    }

    @JvmInline
    value class QrCodeData(private val packed: Pair<Int, String>) {
        val version get() = packed.first
        val data get() = packed.second
    }

    @JvmInline
    value class PollInfo(private val packed: Pair<Long, ByteString>) {
        val clientId get() = packed.first
        val requestId get() = packed.second
    }

    @Serializable
    class JwtToken (
        val sub: String
    )

    fun registerLogonAttemptListener(listener: LogonAttemptListener) {
        onLogonAttemptCallbacks.add(listener)
    }

    fun unregisterLogonAttemptListener(listener: LogonAttemptListener) {
        onLogonAttemptCallbacks.remove(listener)
    }

    fun interface LogonAttemptListener {
        fun onAttempt(id: SteamId)
    }
}