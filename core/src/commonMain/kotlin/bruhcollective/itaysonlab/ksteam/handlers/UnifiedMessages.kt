package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobDroppedException
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobRemoteException
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobTimeoutException
import com.squareup.wire.*
import io.ktor.client.call.*
import kotlinx.coroutines.runBlocking
import okio.Timeout
import kotlin.coroutines.cancellation.CancellationException

/**
 * Clients and kSteam use this handler to use Steam's Web API.
 *
 * **NOTE:** This class can be used as a Wire's [GrpcClient] to be injected inside kSteam's generated RPC services.
 */
class UnifiedMessages internal constructor(
    private val steamClient: SteamClient
) : GrpcClient() {
    /**
     * Create a Unified Message request using the Steam network transport. It is generally recommended to use [GrpcClient] infrastructure instead, unless you want to execute method while you are not signed in.
     *
     * @param signed if this request should be done on current account scope
     * @param methodName formatted as "Service.Method", like "Authentication.BeginAuthSessionViaQR"
     * @param requestAdapter Wire adapter for request body
     * @param responseAdapter Wire adapter for response body
     * @param requestData request body
     */
    @Suppress("DEPRECATION")
    @Throws(CMJobDroppedException::class, CMJobTimeoutException::class, CMJobRemoteException::class, CancellationException::class)
    suspend fun <Request: Any, Response: Any> execute(
        signed: Boolean = true,
        methodName: String,
        requestAdapter: ProtoAdapter<Request>,
        responseAdapter: ProtoAdapter<Response>,
        requestData: Request
    ): Response {
        val packet = SteamPacket.newProto(
            messageId = if (signed) {
                EMsg.k_EMsgServiceMethodCallFromClient
            } else {
                EMsg.k_EMsgServiceMethodCallFromClientNonAuthed
            },
            adapter = requestAdapter,
            payload = requestData
        ).withHeader {
            (this as SteamPacketHeader.Protobuf).targetJobName = "$methodName#1"
        }

        return steamClient.awaitProto(packet, responseAdapter)
    }

    // Wire gPRC notes

    override fun <S : Any, R : Any> newCall(method: GrpcMethod<S, R>): GrpcCall<S, R> {
        return SteamGrpcCall(runtime = this, method = method)
    }

    override fun <S : Any, R : Any> newStreamingCall(method: GrpcMethod<S, R>): GrpcStreamingCall<S, R> {
        error("Streaming calls are not supported in kSteam Wire/gRPC client")
    }

    override fun <S : Any, R : Any> newClientStreamingCall(method: GrpcMethod<S, R>): GrpcClientStreamingCall<S, R> {
        error("Streaming calls are not supported in kSteam Wire/gRPC client")
    }

    override fun <S : Any, R : Any> newServerStreamingCall(method: GrpcMethod<S, R>): GrpcServerStreamingCall<S, R> {
        error("Streaming calls are not supported in kSteam Wire/gRPC client")
    }

    //

    class SteamGrpcCall <S: Any, R: Any> (
        private val runtime: UnifiedMessages,
        override val method: GrpcMethod<S, R>,
    ): GrpcCall<S, R> {
        internal companion object {
            const val AnonymousMarker = "ks_anon"
            const val WebMarker = "ks_web"
        }

        private var cancelled = false
        private var executed = false

        override var requestMetadata: Map<String, String> = mapOf()
        override val responseMetadata: Map<String, String>? = null
        override val timeout: Timeout = Timeout.NONE

        override fun cancel() {
            cancelled = true
            // TODO: cancel job for non-coroutine calls
        }

        override fun clone(): GrpcCall<S, R> = SteamGrpcCall(runtime, method)
        override fun isCanceled(): Boolean = cancelled
        override fun isExecuted(): Boolean = executed

        override fun executeBlocking(request: S): R = runBlocking { execute(request) }

        override suspend fun execute(request: S): R {
            executed = true

            val methodName = method.path.removePrefix("/").replace("/", ".")

            return if (runtime.steamClient.cmNetworkEnabled().not() || requestMetadata.getOrElse(WebMarker) { "0" } == "1") {
                webTransportImpl(methodName, request)
            } else {
                steamTransportImpl(methodName, request)
            }
        }

        private suspend fun steamTransportImpl(methodName: String, request: S): R {
            return runtime.execute(
                signed = requestMetadata.getOrElse(AnonymousMarker) { "0" } == "0",
                methodName = methodName,
                requestAdapter = method.requestAdapter,
                responseAdapter = method.responseAdapter,
                requestData = request
            )
        }

        private suspend fun webTransportImpl(methodName: String, request: S): R {
            val (service, name) = methodName.split(".").let { split ->
                split[0] to split[1]
            }

            val form = runtime.steamClient.webApi.submitProtobufForm(
                path = "I${service}Service/${name}/v1",
                data = method.requestAdapter.encodeByteString(request).base64()
            )

            return form.body<ByteArray>().let(method.responseAdapter::decode)
        }

        override fun enqueue(request: S, callback: GrpcCall.Callback<S, R>) {
            TODO("Non-coroutine usage is currently unsupported")
        }
    }
}