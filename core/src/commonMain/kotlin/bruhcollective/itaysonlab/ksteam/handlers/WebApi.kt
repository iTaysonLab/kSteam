package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import steam.enums.EMsg

class WebApi(
    private val steamClient: SteamClient
): BaseHandler {
    suspend fun <Request: Message<Request, *>, Response: Message<Response, *>> execute(
        signed: Boolean = true,
        methodName: String,
        requestAdapter: ProtoAdapter<Request>,
        responseAdapter: ProtoAdapter<Response>,
        requestData: Request
    ): Response {
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