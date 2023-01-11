import bruhcollective.itaysonlab.ksteam.SteamClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class KSteamClient: CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private val steamClient = SteamClient()

    fun start() {
        runBlocking {
            steamClient.start()
        }
    }
}