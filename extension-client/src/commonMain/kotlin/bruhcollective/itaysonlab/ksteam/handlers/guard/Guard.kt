package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.extension.plugins.SteamGuardPlugin
import bruhcollective.itaysonlab.ksteam.guard.GuardInstance
import bruhcollective.itaysonlab.ksteam.guard.clock.GuardClockContextImpl
import bruhcollective.itaysonlab.ksteam.guard.models.GuardStructure
import bruhcollective.itaysonlab.ksteam.guard.models.SgCreationFlowState
import bruhcollective.itaysonlab.ksteam.guard.models.toConfig
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.configuration
import bruhcollective.itaysonlab.ksteam.handlers.unifiedMessages
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.util.SteamRpcException
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import steam.webui.twofactor.*

/**
 * Steam Guard provider.
 */
class Guard(
    private val steamClient: SteamClient,
) : BaseHandler, SteamGuardPlugin {
    private val twoFactor by lazy {
        GrpcTwoFactor(steamClient.unifiedMessages)
    }

    private val storage = GuardStorage(steamClient)
    private val lazyInstances = mutableMapOf<SteamId, GuardInstance>()

    private val sgAddFlow = MutableStateFlow<SgCreationFlowState>(SgCreationFlowState.Idle)
    val guardConfigurationFlow = sgAddFlow.asStateFlow()

    fun instanceForCurrentUser() = instanceFor(steamClient.currentSessionSteamId)

    /**
     * Request a Steam Guard instance for a [steamId].
     *
     * Returns null if kSteam has no auth information for this [steamId].
     */
    fun instanceFor(steamId: SteamId): GuardInstance? {
        return lazyInstances.getOrPut(steamId) {
            GuardInstance(
                steamId = steamId,
                configuration = storage.queryStructure(steamId) ?: return null,
                clockContext = GuardClockContextImpl(steamClient)
            )
        }
    }

    /**
     * Creates a [SgCreationFlowState] flow.
     *
     * To obtain it, use [guardConfigurationFlow].
     */
    suspend fun initializeSgCreation() {
        sgAddFlow.value = try {
            val response = twoFactor.AddAuthenticator().executeSteam(
                CTwoFactor_AddAuthenticator_Request(
                    steamid = steamClient.currentSessionSteamId.longId,
                    authenticator_type = 1,
                    sms_phone_id = "1",
                    version = 2,
                    device_identifier = steamClient.configuration.getUuid()
                )
            )

            SgCreationFlowState.SmsSent(
                hint = response.phone_number_hint.orEmpty(),
                moving = false,
                guardConfiguration = response.toConfig()
            )
        } catch (sre: SteamRpcException) {
            if (sre.result == EResult.DuplicateRequest) {
                SgCreationFlowState.AlreadyHasGuard
            } else {
                throw sre
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
        twoFactor.RemoveAuthenticatorViaChallengeStart().executeSteam(
            CTwoFactor_RemoveAuthenticatorViaChallengeStart_Request()
        )

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
            twoFactor.RemoveAuthenticatorViaChallengeContinue().executeSteam(
                data = CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request(
                    sms_code = code,
                    version = 2,
                    generate_new_token = true
                )
            ).let {
                if (it.success == true) {
                    it.replacement_token?.toConfig()
                } else {
                    null
                }
            }
        } else {
            twoFactor.FinalizeAddAuthenticator().executeSteam(
                data = CTwoFactor_FinalizeAddAuthenticator_Request(
                    steamid = steamClient.currentSessionSteamId.longId,
                    activation_code = code,
                    validate_sms_code = true
                )
            ).let {
                if (it.success == true) {
                    if (it.want_more == true) {
                        val pair = instance!!.generateCodeWithTime()

                        twoFactor.FinalizeAddAuthenticator().executeSteam(
                            data = CTwoFactor_FinalizeAddAuthenticator_Request(
                                steamid = steamClient.currentSessionSteamId.longId,
                                authenticator_code = pair.codeString,
                                authenticator_time = pair.generationTime
                            )
                        )
                    }

                    previous.guardConfiguration!!
                } else {
                    null
                }
            }
        }

        return if (guardConfiguration != null) {
            storage.writeStructure(steamClient.currentSessionSteamId, guardConfiguration)

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
        storage.writeStructure(steamId, configuration)
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
    suspend fun delete(
        steamId: SteamId,
        code: String? = null,
        removeSgCookies: Boolean = false,
        unsafe: Boolean = false
    ): Int {
        val revocationCode = if (code.isNullOrEmpty()) {
            instanceFor(steamId)?.revocationCode
        } else {
            code
        } ?: return 0

        try {
            val result = twoFactor.RemoveAuthenticator().executeSteam(
                anonymous = code != null,
                data = CTwoFactor_RemoveAuthenticator_Request(
                    revocation_code = revocationCode,
                    revocation_reason = 1,
                    steamguard_scheme = 1,
                    remove_all_steamguard_cookies = removeSgCookies
                )
            )

            val deleteData = result.success == true || unsafe

            if (deleteData) {
                storage.deleteStructure(steamId)
                lazyInstances.remove(steamId)
            }

            return result.revocation_attempts_remaining ?: 0
        } catch (e: Exception) {
            return 0
        }
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit
    override suspend fun getCodeFor(account: SteamId) = instanceFor(account)?.generateCodeWithTime()?.codeString
}