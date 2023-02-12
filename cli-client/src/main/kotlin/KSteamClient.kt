
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.debug.Logging
import bruhcollective.itaysonlab.ksteam.debug.LoggingTransport
import bruhcollective.itaysonlab.ksteam.debug.LoggingVerbosity
import bruhcollective.itaysonlab.ksteam.handlers.account
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds

class KSteamClient: CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private val steamClient = SteamClient(config = SteamClientConfiguration(
        rootFolder = File(Paths.get("").toAbsolutePath().toString(), "ksteam"),
        sqlDriver = JdbcSqliteDriver("jdbc:sqlite:${File(File(Paths.get("").toAbsolutePath().toString(), "ksteam"), "database")}")
    ))

    fun start() {
        runBlocking {
            Logging.transport = object: LoggingTransport {
                override var verbosity: LoggingVerbosity
                    get() = LoggingVerbosity.Verbose
                    set(value) {}

                override fun printError(tag: String, message: String) {
                    println("[$tag] $message")
                }

                override fun printWarning(tag: String, message: String) {
                    println("[$tag] $message")
                }

                override fun printDebug(tag: String, message: String) {
                    println("[$tag] $message")
                }

                override fun printVerbose(tag: String, message: String) {
                    println("[$tag] $message")
                }
            }

            steamClient.start()
            steamClient.account.awaitSignIn()

            delay(2000L)

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

            delay(30.seconds)
        }
    }
}