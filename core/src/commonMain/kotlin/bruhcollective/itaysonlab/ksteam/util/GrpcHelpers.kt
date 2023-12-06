package bruhcollective.itaysonlab.ksteam.util

import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages.SteamGrpcCall
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import com.squareup.wire.GrpcCall
import okio.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Marks that the following [GrpcCall] should be executed as a non-authenticated one. Only affects [SteamGrpcCall] requests.
 */
fun <S: Any, R: Any> GrpcCall<S, R>.markAsNonAuthed() = apply {
    require(this is SteamGrpcCall<S, R>) { "This method is only applicable to kSteam gRPC calls!" }
    requestMetadata = mapOf("ks_anon" to "1")
}

/**
 * This exception is thrown if [SteamGrpcCall]'s execute method returns unsuccessful result.
 */
class SteamRpcException(
    val result: EResult
): Exception()

/**
 * Executes this [GrpcCall] as a Steam call.
 *
 * Differences from an arbitrary execution:
 * - auto-check for using [SteamGrpcCall]
 * - possibility to set non-authed request without explicitly calling [markAsNonAuthed]
 * - API-defined [SteamRpcException] for non-standard results
 */
@Throws(SteamRpcException::class, IOException::class, CancellationException::class)
suspend fun <S: Any, R: Any> GrpcCall<S, R>.executeSteam(
    data: S,
    authorized: Boolean = false
): R {
    if (authorized) {
        markAsNonAuthed()
    }

    return execute(data)
}