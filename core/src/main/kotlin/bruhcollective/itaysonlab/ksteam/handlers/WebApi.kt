package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.Result
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter

class WebApi(
    private val steamClient: SteamClient
): BaseHandler {
    /**
     * Create a WebAPI request using the Steam network transport
     *
     * @param signed if this request should be done on current account scope
     * @param methodName formatted as "Service.Method", like "Authentication.BeginAuthSessionViaQR"
     * @param requestAdapter Wire adapter for request body
     * @param responseAdapter Wire adapter for response body
     * @param requestData request body
     */
    suspend fun <Request: Message<Request, *>, Response: Message<Response, *>> execute(
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
}