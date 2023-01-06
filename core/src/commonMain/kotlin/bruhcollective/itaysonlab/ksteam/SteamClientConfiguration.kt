package bruhcollective.itaysonlab.ksteam

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SteamClientConfiguration (
    private val httpProxyIp: String? = null,
    private val httpProxyPort: Int = 80,
) {
    internal val networkClient = HttpClient(CIO) {
        engine {
            if (httpProxyIp.isNullOrEmpty().not()) {
                @Suppress("HttpUrlsUsage")
                proxy = ProxyBuilder.http("http://$httpProxyIp:$httpProxyPort")
            }
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }

        install(WebSockets) {

        }
    }
}