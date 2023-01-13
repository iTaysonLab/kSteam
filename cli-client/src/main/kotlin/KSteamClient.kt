import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.handlers.Account
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Paths

class KSteamClient: CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default) {
    private val steamClient = SteamClient(config = SteamClientConfiguration(
        rootFolder = File(Paths.get("").toAbsolutePath().toString(), "ksteam")
    ))

    fun start() {
        runBlocking {
            steamClient.start()
            steamClient.getHandler<Account>().apply {
                if (trySignInSaved()) {
                    awaitSignIn()
                    delay(5000L)
                }
            }
        }
    }
}