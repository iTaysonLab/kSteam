package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.extension.plugins.SteamGuardPlugin
import bruhcollective.itaysonlab.ksteam.guard.GuardInstance
import bruhcollective.itaysonlab.ksteam.guard.clock.GuardClockContextImpl
import bruhcollective.itaysonlab.ksteam.guard.models.GuardStructure
import bruhcollective.itaysonlab.ksteam.guard.models.SgCreationFlowState
import bruhcollective.itaysonlab.ksteam.guard.models.toConfig
import bruhcollective.itaysonlab.ksteam.guardMoveConfirm
import bruhcollective.itaysonlab.ksteam.guardMoveStart
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.account
import bruhcollective.itaysonlab.ksteam.handlers.storage
import bruhcollective.itaysonlab.ksteam.handlers.unifiedMessages
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.platform.provideOkioFilesystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import steam.webui.twofactor.CTwoFactor_AddAuthenticator_Request
import steam.webui.twofactor.CTwoFactor_AddAuthenticator_Response
import steam.webui.twofactor.CTwoFactor_FinalizeAddAuthenticator_Request
import steam.webui.twofactor.CTwoFactor_FinalizeAddAuthenticator_Response
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticator_Request
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticator_Response

/**
 * Steam Guard provider.
 */
class Guard(
    private val steamClient: SteamClient,
) : BaseHandler, SteamGuardPlugin {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val lazyInstances = mutableMapOf<SteamId, GuardInstance>()

    private val sgAddFlow = MutableStateFlow<SgCreationFlowState>(SgCreationFlowState.Idle)
    val guardConfigurationFlow = sgAddFlow.asStateFlow()

    fun instanceForCurrentUser() = instanceFor(steamClient.currentSessionSteamId)

    /**
     * Request a Steam Guard instance for a [steamId].
     *
     * Returns null if kSteam has no auth information for this [steamId].
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun instanceFor(steamId: SteamId): GuardInstance? {
        lazyInstances[steamId]?.let {
            return it
        }

        val file = getGuardFile(steamId)

        return provideOkioFilesystem().takeIf { fs ->
            fs.exists(file)
        }?.read(file) {
            GuardInstance(
                steamId = steamId,
                configuration = json.decodeFromBufferedSource(this),
                clockContext = GuardClockContextImpl(steamClient)
            ).also { lazyInstances[steamId] = it }
        }
    }

    /**
     * Creates a [SgCreationFlowState] flow.
     */
    suspend fun initializeSgCreation() {
        sgAddFlow.value = steamClient.unifiedMessages.execute(
            methodName = "TwoFactor.AddAuthenticator",
            requestAdapter = CTwoFactor_AddAuthenticator_Request.ADAPTER,
            responseAdapter = CTwoFactor_AddAuthenticator_Response.ADAPTER,
            requestData = CTwoFactor_AddAuthenticator_Request(
                steamid = steamClient.currentSessionSteamId.longId,
                authenticator_type = 1,
                sms_phone_id = "1",
                version = 2,
                device_identifier = configuration.uuid
            )
        ).data.let { response ->
            if (response.status == EResult.DuplicateRequest.encoded) {
                SgCreationFlowState.AlreadyHasGuard
            } else {
                SgCreationFlowState.SmsSent(
                    hint = response.phone_number_hint.orEmpty(),
                    moving = false,
                    guardConfiguration = response.toConfig(steamClient.currentSessionSteamId)
                )
            }
        }
    }

    /**
     * Resets Steam Guard creation.
     */
    fun resetSgCreation() {
        sgAddFlow.value = SgCreationFlowState.Idle
    }

    /**
     * Confirms moving Steam Guard to another account.
     *
     * This will send an SMS to a phone, from which you need to extract the code and send it to the server.
     */
    suspend fun confirmMove() {
        steamClient.webApi.guardMoveStart(accessToken = steamClient.account.getCurrentAccount()!!.accessToken)
        sgAddFlow.value = SgCreationFlowState.SmsSent(hint = "", moving = true, guardConfiguration = null)
    }

    /**
     * This will confirm a move/add request by a code from the SMS.
     *
     * @return if guard was successfully set up
     */
    suspend fun confirmSgConfiguration(code: String): Boolean {
        val previous = sgAddFlow.value as? SgCreationFlowState.SmsSent ?: return false

        val instance = previous.guardConfiguration?.let {
            GuardInstance(
                steamClient.currentSessionSteamId,
                it,
                GuardClockContextImpl(steamClient)
            )
        }

        val guardConfiguration = if (previous.moving) {
            steamClient.webApi.guardMoveConfirm(
                accessToken = steamClient.account.getCurrentAccount()!!.accessToken,
                obj = CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request(
                    sms_code = code,
                    version = 2,
                    generate_new_token = true
                )
            )?.let {
                if (it.success == true) {
                    it.replacement_token?.toConfig()
                } else {
                    null
                }
            }
        } else {
            steamClient.unifiedMessages.execute(
                methodName = "TwoFactor.FinalizeAddAuthenticator",
                requestAdapter = CTwoFactor_FinalizeAddAuthenticator_Request.ADAPTER,
                responseAdapter = CTwoFactor_FinalizeAddAuthenticator_Response.ADAPTER,
                requestData = CTwoFactor_FinalizeAddAuthenticator_Request(
                    steamid = steamClient.currentSessionSteamId.longId,
                    activation_code = code,
                    validate_sms_code = true
                )
            ).data.let {
                if (it.success == true) {

                    if (it.want_more == true) {
                        val pair = instance!!.generateCodeWithTime()

                        steamClient.unifiedMessages.execute(
                            methodName = "TwoFactor.FinalizeAddAuthenticator",
                            requestAdapter = CTwoFactor_FinalizeAddAuthenticator_Request.ADAPTER,
                            responseAdapter = CTwoFactor_FinalizeAddAuthenticator_Response.ADAPTER,
                            requestData = CTwoFactor_FinalizeAddAuthenticator_Request(
                                steamid = steamClient.currentSessionSteamId.longId,
                                authenticator_code = pair.codeString,
                                authenticator_time = pair.generationTime
                            )
                        ).data
                    }

                    previous.guardConfiguration!!
                } else {
                    null
                }
            }
        }

        return if (guardConfiguration != null) {
            writeGuard(steamClient.currentSessionSteamId, guardConfiguration)

            GuardInstance(
                steamClient.currentSessionSteamId,
                guardConfiguration,
                GuardClockContextImpl(steamClient)
            ).let { createdInstance ->
                lazyInstances[steamClient.currentSessionSteamId] = createdInstance
                sgAddFlow.value = SgCreationFlowState.Success(createdInstance.revocationCode)
            }

            true
        } else {
            false
        }
    }

    /**
     * A special "migration" function which will explicitly add GuardConfiguration.
     *
     * **WARNING:** THIS WILL REPLACE THE CURRENT CONFIG IF IT WAS SUPPLIED!
     */
    fun tryAddConfig(steamId: SteamId, configuration: GuardStructure) {
        lazyInstances[steamId] = GuardInstance(steamId, configuration, GuardClockContextImpl(steamClient))
        writeGuard(steamId, configuration)
    }

    /**
     * Deletes the SG instance from both the Steam servers and local device.
     *
     * This will apply a 15-day trade restriction on the account.
     *
     * @param code manual code for revocation ("I don't have access to Steam Guard"), if null - kSteam will try to use local authenticator
     * @param unsafe force local data deletion even if code was invalid
     * @param removeSgCookies remove all Steam Guard cookies
     * @return revocation attempts left
     */
    suspend fun delete(steamId: SteamId, code: String? = null, removeSgCookies: Boolean = false, unsafe: Boolean = false): Int {
        val revocationCode = if (code.isNullOrEmpty()) {
            instanceFor(steamId)?.revocationCode
        } else {
            code
        } ?: return 0

        // TODO: set steamid
        steamClient.unifiedMessages.execute(
            signed = code == null,
            methodName = "TwoFactor.RemoveAuthenticator",
            requestAdapter = CTwoFactor_RemoveAuthenticator_Request.ADAPTER,
            responseAdapter = CTwoFactor_RemoveAuthenticator_Response.ADAPTER,
            requestData = CTwoFactor_RemoveAuthenticator_Request(
                revocation_code = revocationCode,
                revocation_reason = 1,
                steamguard_scheme = 1,
                remove_all_steamguard_cookies = removeSgCookies
            )
        ).dataNullable.let {
            val deleteData = it?.success == true || unsafe

            if (deleteData) {
                try {
                    provideOkioFilesystem().delete(getGuardFile(steamId), mustExist = false)
                    lazyInstances.remove(steamId)
                } catch (_: Exception) {}
            }

            return it?.revocation_attempts_remaining ?: 0
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeGuard(steamId: SteamId, configuration: GuardStructure) {
        provideOkioFilesystem().write(getGuardFile(steamId)) {
            json.encodeToBufferedSink(configuration, this)
        }
    }

    private fun getGuardFile(steamId: SteamId) = steamClient.storage.storageFor(steamId) / "guard.json"

    override suspend fun onEvent(packet: SteamPacket) = Unit
    override suspend fun getCodeFor(account: SteamId) = instanceFor(account)?.generateCodeWithTime()?.codeString
}