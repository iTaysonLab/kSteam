import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import steam.enums.EMsg
import steam.messages.auth.CAuthentication_BeginAuthSessionViaQR_Request
import steam.messages.auth.CAuthentication_BeginAuthSessionViaQR_Response
import steam.messages.auth.CAuthentication_DeviceDetails
import steam.messages.auth.CAuthentication_PollAuthSessionStatus_Request

class KSteamClient: CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private val steamClient = SteamClient()

    fun start() {
        /*launch {
            steamClient
                .incomingPacketsFlow
                .map { async {
                    println("Received ${it.messageId}")
                }}.buffer()
        }*/

        runBlocking {
            steamClient.start()

            println("Sending message")

            val responsePacket = steamClient.execute(SteamPacket.newProto(
                messageId = EMsg.k_EMsgServiceMethodCallFromClientNonAuthed,
                adapter = CAuthentication_BeginAuthSessionViaQR_Request.ADAPTER,
                payload = CAuthentication_BeginAuthSessionViaQR_Request(
                    device_details = CAuthentication_DeviceDetails(
                        device_friendly_name = "kSteam-JVMTest"
                    ), website_id = "Client"
                )
            ).apply {
                (header as SteamPacketHeader.Protobuf).targetJobName = "Authentication.BeginAuthSessionViaQR#1"
            })

            println(responsePacket.getProtoPayload(CAuthentication_BeginAuthSessionViaQR_Response.ADAPTER))
        }
    }
}