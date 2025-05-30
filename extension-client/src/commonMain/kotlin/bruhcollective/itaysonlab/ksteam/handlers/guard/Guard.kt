package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.guard.GuardInstance
import bruhcollective.itaysonlab.ksteam.guard.clock.GuardClockContextImpl
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.models.guard.GuardStructure
import bruhcollective.itaysonlab.ksteam.models.guard.SgCreationResult
import bruhcollective.itaysonlab.ksteam.models.guard.SgDeletionResult
import bruhcollective.itaysonlab.ksteam.models.guard.toConfig
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobRemoteException
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import steam.webui.twofactor.*

/**
 * Steam Guard provider.
 */
class Guard(
    private val steamClient: ExtendedSteamClient,
) {
    private val storage = GuardStorage(steamClient.client)
    private val lazyInstances = mutableMapOf<SteamId, GuardInstance>()

    fun instanceForCurrentUser() = instanceFor(steamClient.currentSessionSteamId.takeIf {
        it.longId != 0L
    } ?: steamClient.configuration.autologinSteamId)

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
                clockContext = GuardClockContextImpl(steamClient.client)
            )
        }
    }

    /**
     * Initializes Steam Guard adding process for the current logged in SteamID.
     */
    suspend fun initializeSgCreation(): SgCreationResult {
        return try {
            val response = steamClient.grpc.twoFactor.AddAuthenticator().executeSteam(
                CTwoFactor_AddAuthenticator_Request(
                    steamid = steamClient.currentSessionSteamId.longId,
                    authenticator_type = 1,
                    version = 2,
                    device_identifier = steamClient.configuration.getUuid()
                ), web = true
            )

            when (response.status) {
                EResult.OK.encoded -> {
                    SgCreationResult.AwaitingConfirmation(
                        hint = response.phone_number_hint.orEmpty(),
                        moving = false,
                        guardConfiguration = response.toConfig(),
                        isEmail = response.confirm_type != 0
                    )
                }

                EResult.DuplicateRequest.encoded -> {
                    SgCreationResult.AlreadyHasGuard
                }

                else -> {
                    SgCreationResult.Error(EResult.byEncoded(response.status ?: EResult.Fail.encoded))
                }
            }
        } catch (sre: CMJobRemoteException) {
            if (sre.result == EResult.DuplicateRequest) {
                SgCreationResult.AlreadyHasGuard
            } else {
                SgCreationResult.Error(sre.result)
            }
        }
    }

    /**
     * Initializes Steam Guard moving process for the current logged in SteamID. This will only work if Steam Guard is already present on account.
     *
     * This will send an SMS to a phone, from which you need to extract the code and send it to the server.
     */
    suspend fun initializeSgMoving(): SgCreationResult {
        return try {
            steamClient.grpc.twoFactor.RemoveAuthenticatorViaChallengeStart().executeSteam(data = CTwoFactor_RemoveAuthenticatorViaChallengeStart_Request(), web = true)
            SgCreationResult.AwaitingConfirmation(moving = true)
        } catch (sre: CMJobRemoteException) {
            SgCreationResult.Error(sre.result)
        }
    }

    /**
     * This will confirm a move request by a code from the SMS.
     *
     * @param code the SMS code for confirming the action
     * @param structure the [GuardStructure] obtained from [SgCreationResult.AwaitingConfirmation]
     *
     * @return if guard was successfully set up
     */
    suspend fun confirmSgCreation(code: String, structure: GuardStructure): GuardInstance? {
        val instance = GuardInstance(steamClient.currentSessionSteamId, structure, GuardClockContextImpl(steamClient.client))
        val firstPair = instance.generateCodeWithTime()

        return steamClient.grpc.twoFactor.FinalizeAddAuthenticator().executeSteam(
            web = true,
            data = CTwoFactor_FinalizeAddAuthenticator_Request(
                steamid = steamClient.currentSessionSteamId.longId,
                activation_code = code,
                validate_sms_code = true,
                authenticator_code = firstPair.codeString,
                authenticator_time = firstPair.generationTime
            )
        ).takeIf { it.success == true }?.let {
            if (it.want_more == true) {
                val (secondCode, secondTime) = instance.generateCodeWithTime()

                steamClient.grpc.twoFactor.FinalizeAddAuthenticator().executeSteam(
                    web = true,
                    data = CTwoFactor_FinalizeAddAuthenticator_Request(
                        steamid = steamClient.currentSessionSteamId.longId,
                        authenticator_code = secondCode,
                        authenticator_time = secondTime
                    )
                )
            } else {
                it
            }
        }?.let {
            tryAddConfig(steamClient.currentSessionSteamId, structure)
        }
    }

    /**
     * This will confirm a moving request by a code from the SMS.
     *
     * @param code the SMS code for confirming the action
     * @return if guard was successfully set up
     */
    suspend fun confirmSgMoving(code: String): GuardInstance? {
        val structure = runCatching {
            steamClient.grpc.twoFactor.RemoveAuthenticatorViaChallengeContinue().executeSteam(
                data = CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request(sms_code = code, version = 2, generate_new_token = true),
                web = true
            ).replacement_token!!.toConfig()
        }.getOrNull() ?: return null

        return tryAddConfig(steamClient.currentSessionSteamId, structure)
    }

    /**
     * A special "migration" function which will explicitly add GuardConfiguration.
     *
     * **WARNING:** THIS WILL REPLACE THE CURRENT CONFIG IF IT WAS SUPPLIED!
     */
    fun tryAddConfig(steamId: SteamId, configuration: GuardStructure): GuardInstance {
        storage.writeStructure(steamId, configuration)

        return GuardInstance(steamId, configuration, GuardClockContextImpl(steamClient.client)).also {
            lazyInstances[steamId] = it
        }
    }

    /**
     * Deletes the SG instance from both the Steam servers and local device.
     *
     * This will apply a 15-day trade restriction on the account.
     *
     * @param code manual code for revocation ("I don't have access to Steam Guard"), if null - kSteam will try to use local authenticator
     * @param unsafe force local data deletion even if code was invalid
     * @param removeSgCookies remove all Steam Guard cookies
     *
     * @return the status of deleting process
     */
    suspend fun delete(
        steamId: SteamId,
        code: String? = null,
        removeSgCookies: Boolean = false,
        unsafe: Boolean = false
    ): SgDeletionResult {
        val revocationCode = if (code.isNullOrEmpty()) {
            instanceFor(steamId)?.revocationCode
        } else {
            code
        } ?: return SgDeletionResult.UnsupportedOperation

        val result = try {
            steamClient.grpc.twoFactor.RemoveAuthenticator().executeSteam(
                anonymous = code != null,
                web = true,
                data = CTwoFactor_RemoveAuthenticator_Request(
                    revocation_code = revocationCode,
                    revocation_reason = 1,
                    steamguard_scheme = 1,
                    remove_all_steamguard_cookies = removeSgCookies
                )
            )
        } catch (e: CMJobRemoteException) {
            return SgDeletionResult.Error(e.result)
        }

        val deleteData = result.success == true || unsafe

        if (deleteData) {
            storage.deleteStructure(steamId)
            lazyInstances.remove(steamId)
        }

        return if (result.success == true) {
            SgDeletionResult.Success
        } else {
            SgDeletionResult.InvalidCode(result.revocation_attempts_remaining ?: 0)
        }
    }

    suspend fun getCodeFor(account: SteamId) = instanceFor(account)?.generateCodeWithTime()?.codeString
}