import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import kotlinx.coroutines.*

class KSteamClient: CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private val steamClient = SteamClient()

    fun start() {
        runBlocking {
            println("Client start")
            steamClient.start()
            println("Client ready")
        }
    }
}