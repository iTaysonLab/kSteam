
import bruhcollective.itaysonlab.ksteam.Core
import bruhcollective.itaysonlab.ksteam.debug.KSteamLoggingVerbosity
import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.debug.StdoutLoggingTransport
import bruhcollective.itaysonlab.ksteam.handlers.News
import bruhcollective.itaysonlab.ksteam.handlers.account
import bruhcollective.itaysonlab.ksteam.handlers.news
import bruhcollective.itaysonlab.ksteam.kSteam
import bruhcollective.itaysonlab.ksteam.models.SteamId
import kotlinx.coroutines.*
import okio.Path.Companion.toOkioPath
import java.io.File
import java.nio.file.Paths

class KSteamClient: CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {

    private val steamClient = kSteam {
        loggingTransport = StdoutLoggingTransport
        loggingVerbosity = KSteamLoggingVerbosity.Verbose

        rootFolder = File(Paths.get("").toAbsolutePath().toString(), "ksteam").toOkioPath()

        install(Core) {

        }
    }

    fun start() {
        runBlocking {
            steamClient.dumperMode = PacketDumper.DumpMode.Full
            steamClient.start()

            steamClient.account.awaitSignIn()

            delay(2000L)

            steamClient.news.getEventDetails(
                eventIds = listOf(3723960129346707600),
                clanIds = listOf(SteamId(103582791440160998_u))
            )

            steamClient.news.getUserNews(
                showEvents = News.UserNewsFilterScenario.FriendActivity
            ).forEach {
                println(it)
            }

            /*steamClient.executeAndForget(
                SteamPacket.newProto(messageId = EMsg.k_EMsgClientChangeStatus, adapter = CMsgClientChangeStatus.ADAPTER, payload = CMsgClientChangeStatus(
                    persona_state = 1
                ))
            ).let { println(it) }

            steamClient.getHandler<WebApi>().execute(
                methodName = "Econ.GetInventoryItemsWithDescriptions",
                requestAdapter = CEcon_GetInventoryItemsWithDescriptions_Request.ADAPTER,
                responseAdapter = CEcon_GetInventoryItemsWithDescriptions_Response.ADAPTER,
                requestData = CEcon_GetInventoryItemsWithDescriptions_Request(
                    steamid = 76561199092511963u.toLong(),
                    appid = 730,
                    contextid = 2,
                    get_descriptions = true,
                    language = "english",
                    count = 10
                ),
            ).let { println(it) }*/
        }
    }
}