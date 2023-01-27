package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

class SteamClientConfiguration(
    private val httpProxyIp: String? = null,
    private val httpProxyPort: Int = 80,
    internal val deviceInfo: DeviceInformation = DeviceInformation(),
    internal val rootFolder: File,
    internal val language: ELanguage = ELanguage.English,
) {
    init {
        rootFolder.mkdirs()
    }

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

        install(WebSockets) {

        }
    }

    internal val apiClient = HttpClient(OkHttp) {
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
    }
}