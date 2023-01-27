package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.guard.GuardInstance
import bruhcollective.itaysonlab.ksteam.guard.clock.GuardClockContextImpl
import bruhcollective.itaysonlab.ksteam.guard.models.SgCreationFlowState
import bruhcollective.itaysonlab.ksteam.guard.models.toConfig
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.account
import bruhcollective.itaysonlab.ksteam.handlers.storage
import bruhcollective.itaysonlab.ksteam.handlers.webApi
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.proto.GuardConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.buffer
import okio.sink
import okio.source
import steam.webui.twofactor.*
import java.io.File

/**
 * Steam Guard provider.
 */
class Guard(
    private val steamClient: SteamClient
) : BaseHandler {
    private val lazyInstances = mutableMapOf<SteamId, GuardInstance>()

    private val sgAddFlow = MutableStateFlow<SgCreationFlowState>(SgCreationFlowState.TryingToAdd)
    val guardConfigurationFlow = sgAddFlow.asStateFlow()

    fun instanceForCurrentUser() = instanceFor(steamClient.currentSessionSteamId)

    /**
     * Request a Steam Guard instance for a [steamId].
     *
     * Returns null if kSteam has no auth information for this [steamId].
     */
    fun instanceFor(steamId: SteamId): GuardInstance? {
        lazyInstances[steamId]?.let {
            return it
        }

        return getGuardFile(steamId).takeIf(File::exists)?.let { guardFile ->
            GuardInstance(
                steamId = steamId,
                configuration = guardFile.source().buffer().use {
                    GuardConfiguration.ADAPTER.decode(it)
                }, clockContext = GuardClockContextImpl(steamClient)
            ).also { lazyInstances[steamId] = it }
        }
    }

    /**
     * Creates a [SgCreationFlowState] flow.
     */
    suspend fun initializeSgCreation() {
        sgAddFlow.value = SgCreationFlowState.TryingToAdd
        sgAddFlow.value = steamClient.webApi.execute(
            methodName = "TwoFactor.AddAuthenticator",
            requestAdapter = CTwoFactor_AddAuthenticator_Request.ADAPTER,
            responseAdapter = CTwoFactor_AddAuthenticator_Response.ADAPTER,
            requestData = CTwoFactor_AddAuthenticator_Request(
                steamid = steamClient.currentSessionSteamId.longId,
                authenticator_type = 1,
                sms_phone_id = "1",
                version = 2,
                device_identifier = steamClient.config.deviceInfo.uuid
            )
        ).data.let { response ->
            if (response.status == EResult.DuplicateRequest.encoded) {
                SgCreationFlowState.AlreadyHasGuard(false)
            } else {
                SgCreationFlowState.SmsSent(
                    hint = response.phone_number_hint.orEmpty(),
                    returnedBecauseOfError = false,
                    moving = false,
                    guardConfiguration = response.toConfig(steamClient.currentSessionSteamId)
                )
            }
        }
    }

    /**
     * Confirms moving Steam Guard to another account.
     *
     * This will send a SMS to an phone, from which you need to extract the code and send it to the server.
     */
    suspend fun confirmMove() {
        sgAddFlow.value = SgCreationFlowState.AlreadyHasGuard(true)

        steamClient.externalWebApi.guardMoveStart(
            accessToken = steamClient.account.getCurrentAccount()!!.accessToken
        )

        sgAddFlow.value = SgCreationFlowState.SmsSent(
            hint = "",
            returnedBecauseOfError = false,
            moving = true,
            guardConfiguration = null
        )
    }

    /**
     * This will confirm a move/add request by a code from the SMS.
     */
    suspend fun confirmSgConfiguration(code: String) {
        val previous = sgAddFlow.value as SgCreationFlowState.SmsSent
        val instance = previous.guardConfiguration?.let {
            GuardInstance(
                steamClient.currentSessionSteamId,
                it,
                GuardClockContextImpl(steamClient)
            )
        }

        sgAddFlow.value = SgCreationFlowState.Processing

        val guardConfiguration = if (previous.moving) {
            steamClient.externalWebApi.guardMoveConfirm(
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
            steamClient.webApi.execute(
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

                        steamClient.webApi.execute(
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

        if (guardConfiguration != null) {
            writeGuard(steamClient.currentSessionSteamId, guardConfiguration)

            GuardInstance(
                steamClient.currentSessionSteamId,
                guardConfiguration,
                GuardClockContextImpl(steamClient)
            ).let { createdInstance ->
                lazyInstances[steamClient.currentSessionSteamId] = createdInstance
                sgAddFlow.value = SgCreationFlowState.Success(createdInstance.revocationCode)
            }
        } else {
            sgAddFlow.value = previous.copy(returnedBecauseOfError = true)
        }
    }

    /**
     * A special "migration" function which will explicitly add GuardConfiguration.
     *
     * WARNING: THIS WILL REPLACE THE CURRENT CONFIG IF IT WAS SUPPLIED!
     */
    fun tryAddConfig(steamId: SteamId, configuration: GuardConfiguration) {
        lazyInstances[steamId] = GuardInstance(steamId, configuration, GuardClockContextImpl(steamClient))
        writeGuard(steamId, configuration)
    }

    /**
     * Deletes the SG instance from both the Steam servers and local device.
     *
     * This will apply a 15-day trade restriction on the account.
     *
     * @param code manual code for revocation ("I don't have access to Steam Guard")
     * @param unsafe force local data deletion even if code was invalid
     * @return revocation attempts left
     */
    suspend fun delete(steamId: SteamId, code: String? = null, unsafe: Boolean = false): Int {
        val revocationCode = if (code.isNullOrEmpty()) {
            instanceFor(steamId)?.revocationCode
        } else {
            code
        } ?: return 0

        // TODO: set steamid
        steamClient.webApi.execute(
            signed = code == null,
            methodName = "TwoFactor.FinalizeAddAuthenticator",
            requestAdapter = CTwoFactor_RemoveAuthenticator_Request.ADAPTER,
            responseAdapter = CTwoFactor_RemoveAuthenticator_Response.ADAPTER,
            requestData = CTwoFactor_RemoveAuthenticator_Request(
                revocation_code = revocationCode,
                revocation_reason = 1,
                steamguard_scheme = 1,
                remove_all_steamguard_cookies = false
            )
        ).dataNullable.let {
            val deleteData = it?.success == true || unsafe

            if (deleteData) {
                try {
                    getGuardFile(steamId).delete()
                    lazyInstances.remove(steamId)
                } catch (_: Exception) {}
            }

            return it?.revocation_attempts_remaining ?: 0
        }
    }

    /**
     * A special "migration" function which will import a .mafile from the old Steam Mobile app or apps like SDA.
     */
    suspend fun tryMigrateFromMafile() {
        // TODO
    }

    private fun writeGuard(steamId: SteamId, configuration: GuardConfiguration) {
        getGuardFile(steamId).apply {
            if (!exists()) createNewFile()
        }.sink().buffer().use {
            GuardConfiguration.ADAPTER.encode(it, configuration)
        }
    }

    private fun getGuardFile(steamId: SteamId) = File(steamClient.storage.storageFor(steamId), "guard")

    override suspend fun onEvent(packet: SteamPacket) = Unit
}