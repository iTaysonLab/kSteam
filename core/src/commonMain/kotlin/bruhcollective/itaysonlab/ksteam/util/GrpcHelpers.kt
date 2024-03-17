package bruhcollective.itaysonlab.ksteam.util

import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages.SteamGrpcCall
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import com.squareup.wire.GrpcCall
import okio.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Marks that the following [GrpcCall] should be executed as a non-authenticated one. Only affects [SteamGrpcCall] requests.
 */
private fun <S: Any, R: Any> GrpcCall<S, R>.markAsAnonymous() = apply {
    require(this is SteamGrpcCall<S, R>) { "This method is only applicable to kSteam gRPC calls!" }
    requestMetadata = requestMetadata + (SteamGrpcCall.AnonymousMarker to "1")
}

/**
 * Marks that the following [GrpcCall] should be executed by using a web transport. Only affects [SteamGrpcCall] requests.
 *
 * Why use this method?
 * For some unknown reasons, Steam3 Networking refuses to execute some Unified Messages sent by an internal RPC bridge, returning EResult.Fail with transport_error = 1
 * However, if you execute them by using a web bridge (api.steampowered.com) with the current token, it will continue executing without problems.
 *
 * Affected:
 * TwoFactor.RemoveAuthenticatorViaChallengeStart
 * TwoFactor.RemoveAuthenticatorViaChallengeContinue
 * TwoFactor.RemoveAuthenticator
 * TwoFactor.FinalizeAddAuthenticator
 * (and possibly EVERY TwoFactor request?)
 *
 * **NOTE:** Web-transported methods cannot be forcibly made anonymous.
 */
private fun <S: Any, R: Any> GrpcCall<S, R>.markAsWeb() = apply {
    require(this is SteamGrpcCall<S, R>) { "This method is only applicable to kSteam gRPC calls!" }
    requestMetadata = requestMetadata + (SteamGrpcCall.WebMarker to "1")
}

/**
 * This exception is thrown if [SteamGrpcCall]'s execute method returns unsuccessful result.
 */
class SteamRpcException(
    val method: String,
    val result: EResult
): Exception("Steam RPC method $method failed: $result")

/**
 * Executes this [GrpcCall] as a Steam call.
 *
 * Differences from an arbitrary execution:
 * - auto-check for using [SteamGrpcCall]
 * - possibility to set non-authed request without explicitly calling [markAsAnonymous]
 * - API-defined [SteamRpcException] for non-standard results
 */
@Throws(SteamRpcException::class, IOException::class, CancellationException::class)
suspend fun <S: Any, R: Any> GrpcCall<S, R>.executeSteam(
    data: S,
    anonymous: Boolean = false,
    web: Boolean = false,
): R {
    if (web) {
        markAsWeb()
    }

    if (anonymous) {
        markAsAnonymous()
    }

    return execute(data)
}