package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.Result
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.util.SteamRpcException
import com.squareup.wire.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import okio.Timeout

/**
 * Clients and kSteam use this handler to use Steam's Web API.
 *
 * **NOTE:** This class can be used as a Wire's [GrpcClient] to be injected inside kSteam's generated RPC services.
 */
class UnifiedMessages internal constructor(
    private val steamClient: SteamClient
) : BaseHandler, GrpcClient() {
    /**
     * Create a Unified Message request using the Steam network transport. It is generally recommended to use [GrpcClient] infrastructure instead, unless you want to execute method while you are not signed in.
     *
     * @param signed if this request should be done on current account scope
     * @param methodName formatted as "Service.Method", like "Authentication.BeginAuthSessionViaQR"
     * @param requestAdapter Wire adapter for request body
     * @param responseAdapter Wire adapter for response body
     * @param requestData request body
     */
    suspend fun <Request: Any, Response: Any> execute(
        signed: Boolean = true,
        methodName: String,
        requestAdapter: ProtoAdapter<Request>,
        responseAdapter: ProtoAdapter<Response>,
        requestData: Request
    ): Result<Response> {
        return steamClient.execute(SteamPacket.newProto(
            messageId = if (signed) {
                EMsg.k_EMsgServiceMethodCallFromClient
            } else {
                EMsg.k_EMsgServiceMethodCallFromClientNonAuthed
            },
            adapter = requestAdapter,
            payload = requestData
        ).apply {
            (header as SteamPacketHeader.Protobuf).targetJobName = "$methodName#1"
        }).getProtoPayload(responseAdapter)
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit

    // Wire gPRC notes

    override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
        return SteamGrpcCall(runtime = this, method = method)
    }

    override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> {
        error("Streaming calls are not supported in kSteam Wire/gRPC client")
    }

    //

    class SteamGrpcCall <S: Any, R: Any> (
        private val runtime: UnifiedMessages,
        override val method: GrpcMethod<S, R>,
    ): GrpcCall<S, R> {
        private var cancelled = false

        override var requestMetadata: Map<String, String> = mapOf()
        override val responseMetadata: Map<String, String>? = null
        override val timeout: Timeout = Timeout.NONE

        override fun cancel() {
            cancelled = true
            // TODO: cancel job for non-coroutine calls
        }

        override fun clone(): GrpcCall<S, R> = SteamGrpcCall(runtime, method)
        override fun isCanceled(): Boolean = cancelled

        override fun isExecuted(): Boolean {
            TODO("Not yet implemented")
        }

        override fun executeBlocking(request: S): R = runBlocking {
            execute(request)
        }

        override suspend fun execute(request: S): R {
            val steamResult = runtime.execute(
                signed = requestMetadata.getOrElse("ks_anon") { "0" } == "1",
                methodName = method.path.removePrefix("/").replace("/", "."),
                requestAdapter = method.requestAdapter,
                responseAdapter = method.responseAdapter,
                requestData = request
            )

            if (steamResult.isSuccess) {
                return steamResult.data
            } else {
                throw SteamRpcException(steamResult.result)
            }
        }

        override fun enqueue(request: S, callback: GrpcCall.Callback<S, R>) {
            TODO("Non-coroutine usage is currently unsupported")
        }
    }
}