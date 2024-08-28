package bruhcollective.itaysonlab.ksteam.util

import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages.SteamGrpcCall
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import com.squareup.wire.GrpcCall
import kotlinx.coroutines.CancellationException
import okio.IOException

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
 *
 * @param data raw protobuf message passed as request body
 * @param anonymous should this request be anonymous (skips authorization checks)
 * @param web force REST API transport (TwoFactor methods fail on Steam Network)
 * @throws SteamRpcException if RPC error occurred
 * @throws IOException if network error occurred
 * @throws IllegalArgumentException if [GrpcCall] is not [SteamRpcException]
 * @return response protobuf message
 */
@Throws(SteamRpcException::class, IllegalArgumentException::class, IOException::class, CancellationException::class)
suspend fun <S: Any, R: Any> GrpcCall<S, R>.executeSteam(
    data: S,
    anonymous: Boolean = false,
    web: Boolean = false,
): R {
    require(this is SteamGrpcCall<S, R>) { "executeSteam is only applicable to kSteam gRPC calls!" }

    if (web) {
        markAsWeb()
    }

    if (anonymous) {
        markAsAnonymous()
    }

    return execute(data)
}

suspend fun <S: Any, R: Any> GrpcCall<S, R>.executeSteamOrNull(
    data: S,
    anonymous: Boolean = false,
    web: Boolean = false,
): R? {
    return runCatching {
        executeSteam(data, anonymous = anonymous, web = web)
    }.getOrNull()
}