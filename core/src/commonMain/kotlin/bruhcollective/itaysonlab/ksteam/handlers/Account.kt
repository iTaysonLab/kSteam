package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.extension.plugins.SteamGuardPlugin
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.account.AuthorizationState
import bruhcollective.itaysonlab.ksteam.models.account.SteamAccountAuthorization
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.platform.encryptWithRsa
import bruhcollective.itaysonlab.ksteam.platform.getIpv4Address
import bruhcollective.itaysonlab.ksteam.util.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
) : BaseHandler {
    private companion object {
        const val TAG = "Account"
    }

    private val authenticationClient by lazy { GrpcAuthenticationClient(steamClient.unifiedMessages) }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val pollScope = CreateSupervisedCoroutineScope("authStatePolling", Dispatchers.Default) { _, _ -> }
    private val deviceInfo get() = steamClient.config.deviceInfo

    private var pollInfo: PollInfo? = null
    private var authStateWatcher: Job? = null
    private var authState = MutableStateFlow<AuthorizationState>(AuthorizationState.Unauthorized)
    val clientAuthState = authState.asStateFlow()

    internal var tokenRequested = MutableStateFlow(false)

    /**
     * Gets all necessary data to generate a sign-in QR code which can be scanned from the mobile app or other kSteam instance.
     *
     * Also starts a polling session.
     */
    suspend fun getSignInQrCode(): QrCodeData? {
        val qrData = try {
            authenticationClient.BeginAuthSessionViaQR().executeSteam(
                CAuthentication_BeginAuthSessionViaQR_Request(
                    device_friendly_name = deviceInfo.deviceName,
                    device_details = deviceInfo.toAuthDetails(),
                    platform_type = deviceInfo.platformType.ordinal,
                    website_id = if (deviceInfo.platformType == EAuthTokenPlatformType.k_EAuthTokenPlatformType_MobileApp) {
                        "Mobile"
                    } else {
                        "Unknown"
                    }
                ), authorized = false
            )
        } catch (e: SteamRpcException) {
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
        val rsaData = authenticationClient.GetPasswordRSAPublicKey().executeSteam(
            CAuthentication_GetPasswordRSAPublicKey_Request(account_name = username),
            authorized = false
        )

        val encryptedPassword =
            encryptWithRsa(password, rsaData.publickey_mod.orEmpty(), rsaData.publickey_exp.orEmpty())
                .toByteString().base64().dropLast(1)

        val signInResult = try {
            authenticationClient.BeginAuthSessionViaCredentials().executeSteam(
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
                ), authorized = false
            )
        } catch (e: SteamRpcException) {
            return if (e.result == EResult.InvalidPassword) {
                AuthorizationResult.InvalidPassword
            } else {
                AuthorizationResult.RpcError
            }
        }

        KSteamLogging.logVerbose(TAG) { "[login] success, waiting for 2FA with available types: [${signInResult.allowed_confirmations.joinToString()}]" }

        pollInfo = PollInfo(signInResult.client_id!! to signInResult.request_id!!)
        authState.emit(AuthorizationState.AwaitingTwoFactor(signInResult))

        val mappedConfirmations = signInResult.allowed_confirmations.mapNotNull { EAuthSessionGuardType.fromValue(it.confirmation_type ?: 0) }

        if (mappedConfirmations.let {
                it.contains(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation) || it.contains(EAuthSessionGuardType.k_EAuthSessionGuardType_EmailConfirmation)
            }) {
            createWatcherFlow(signInResult.interval ?: 5f)
        }

        if (mappedConfirmations.contains(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode)) {
            val guardCode = steamClient.getImplementingHandlerOrNull<SteamGuardPlugin>()
                ?.getCodeFor(SteamId(signInResult.steamid?.toULong() ?: 0u))

            if (guardCode != null) {
                KSteamLogging.logVerbose(TAG) { "[login] kSteam persistence has Steam Guard for this SteamID, skipping 2FA..." }
                updateCurrentSessionWithCode(guardCode)
            }
        }

        return AuthorizationResult.Success
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
            authenticationClient.UpdateAuthSessionWithSteamGuardCode().executeSteam(
                CAuthentication_UpdateAuthSessionWithSteamGuardCode_Request(
                    client_id = pollInfo!!.clientId,
                    steamid = authState.steamId.longId,
                    code = code,
                    code_type = authState.sumProtos.filterNot {
                        it == EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation || it == EAuthSessionGuardType.k_EAuthSessionGuardType_EmailConfirmation
                    }.first().value
                ), authorized = false
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
     * You can specify your own SteamID to sign in a particular account. If this parameter is omitted, the "default" SteamID will be selected.
     *
     * @return if there is an account available
     */
    suspend fun trySignInSaved(
        steamId: SteamId = steamClient.configuration.autologinSteamId
    ): Boolean {
        val accountToSignIn = steamClient.configuration.getSecureAccount(steamId)

        return if (accountToSignIn != null) {
            sendClientLogon(steamId = steamId, token = accountToSignIn.refreshToken)
            true
        } else {
            KSteamLogging.logWarning(TAG) { "[autologin] no saved accounts in kSteam persistence, skipping..." }
            false
        }
    }

    private suspend fun sendClientLogon(token: String, steamId: SteamId) {
        if (steamClient.configuration.machineId.isEmpty()) {
            steamClient.configuration.machineId = Random.nextBytes(64).toByteString().hex()
        }

        val sentryFileHash = steamClient.sentry.sentryHash(steamId)

        val currentIp = when (steamClient.config.authPrivateIpLogic) {
            SteamClientConfiguration.AuthPrivateIpLogic.UsePrivateIp -> {
                convertToCmIpV4(getIpv4Address())
            }

            SteamClientConfiguration.AuthPrivateIpLogic.Generate -> {
                convertToCmIpV4(generateIpV4Int())
            }

            SteamClientConfiguration.AuthPrivateIpLogic.None -> null
        }

        steamClient.executeAndForget(SteamPacket.newProto(
            EMsg.k_EMsgClientLogon, CMsgClientLogon.ADAPTER, CMsgClientLogon(
                protocol_version = EnvironmentConstants.PROTOCOL_VERSION,
                client_package_version = 1671236931,
                client_language = steamClient.config.language.vdfName,
                client_os_type = steamClient.config.deviceInfo.osType.encoded,
                should_remember_password = true,
                qos_level = 2,
                machine_id = steamClient.configuration.machineId.decodeHex(),
                machine_name = steamClient.config.deviceInfo.deviceName,
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
     * Cancel authentication polling, if you have used [getSignInQrCode].
     */
    fun cancelPolling() = authStateWatcher?.cancel()

    suspend fun awaitSignIn() = clientAuthState.first { it is AuthorizationState.Success }

    fun hasSavedDataForAtLeastOneAccount(): Boolean {
        return steamClient.configuration.containsSecureAccount(steamClient.configuration.autologinSteamId)
    }

    private suspend fun pollAuthStatus(): CAuthentication_PollAuthSessionStatus_Response {
        require(pollInfo != null) { "pollInfo should not be null" }

        return authenticationClient.PollAuthSessionStatus().executeSteam(
            CAuthentication_PollAuthSessionStatus_Request(
                client_id = pollInfo!!.clientId,
                request_id = pollInfo!!.requestId
            ), authorized = false
        )
    }

    private fun createWatcherFlow(interval: Float) {
        authStateWatcher?.cancel()
        authStateWatcher = flow<Unit> {
            while (true) {
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
            KSteamLogging.logVerbose(TAG) { "[poller] successfully logged in -> $pollAnswer" }

            val steamId = try {
                SteamId(json.decodeFromString<JwtToken>(pollAnswer.refresh_token.split(".")[1].decodeBase64String()).sub.toULong())
            } catch (e: Exception) {
                e.printStackTrace()
                (clientAuthState.value as? AuthorizationState.AwaitingTwoFactor)?.steamId
            }

            if (steamId == null) {
                KSteamLogging.logError(TAG) { "[poller] received JWT, but no Steam ID exposed: couldn't continue signing in" }
                return true
            }

            steamClient.configuration.updateSecureAccount(steamId, SteamAccountAuthorization(
                accessToken = pollAnswer.access_token,
                refreshToken = pollAnswer.refresh_token,
                accountName = pollAnswer.account_name.orEmpty()
            ))

            sendClientLogon(
                token = pollAnswer.refresh_token,
                steamId = steamId
            )

            true
        } else {
            false
        }
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientLogOnResponse -> {
                if (packet.isProtobuf()) {
                    val response = packet.getProtoPayload(CMsgClientLogonResponse.ADAPTER)

                    if (response.data.eresult == EResult.OK.encoded) {
                        // We are logged in to Steam3 server
                        authStateWatcher?.cancel()
                        authState.value = AuthorizationState.Success

                        updateAccessToken()
                        tokenRequested.value = true
                    }
                } else {
                    // Ignore, that's kicked out for inactivity, Restarter should reconnect again
                }
            }

            EMsg.k_EMsgClientLogOff, EMsg.k_EMsgClientLoggedOff -> {
                tokenRequested.value = false
            }

            else -> {}
        }
    }

    fun getDefaultAccount() = steamClient.configuration.getSecureAccount(steamClient.configuration.autologinSteamId)
    fun getCurrentAccount() = steamClient.configuration.getSecureAccount(steamClient.currentSessionSteamId)
    fun buildSteamLoginSecureCookie() = getCurrentAccount()?.let { "${steamClient.currentSessionSteamId}||${it.accessToken}" }.orEmpty()

    suspend fun updateAccessToken() {
        try {
            getCurrentAccount()?.let { account ->
                authenticationClient.GenerateAccessTokenForApp().executeSteam(
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
        } catch (e: SteamRpcException) {
            //
        }
    }

    enum class AuthorizationResult {
        // Now you should use the state from Account.clientAuthState to show TFA interface
        Success,

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
}