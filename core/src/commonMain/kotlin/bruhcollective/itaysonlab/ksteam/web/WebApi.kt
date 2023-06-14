package bruhcollective.itaysonlab.ksteam.web

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import bruhcollective.itaysonlab.ksteam.web.models.GetCMListForConnectResponse
import bruhcollective.itaysonlab.ksteam.web.models.QueryTimeData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.serialization.Serializable

class WebApi(
    private val apiClient: HttpClient,
) {
    val gateway = this[EnvironmentConstants.WEB_API_BASE]
    val community = this[EnvironmentConstants.COMMUNITY_API_BASE]
    val store = this[EnvironmentConstants.STORE_API_BASE]

    suspend fun getCmList(): List<CMServerEntry> {
        // New
        return gateway.method("ISteamDirectory/GetCMListForConnect/v1") {
            "cmtype" with "websockets"
            "realm" with "steamglobal"
            "maxcount" with 1
        }.body<WebApiBoxedResponse<GetCMListForConnectResponse>>().response.servers
    }

    suspend fun getServerTime(): QueryTimeData {
        return gateway.method("ITwoFactorService/QueryTime/v0001") {
            "steamid" with "0"
        }.postBody<WebApiBoxedResponse<QueryTimeData>>().response
    }

    suspend inline fun <reified T> submitFormTyped(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: String,
        parameters: Map<String, String>,
        formParameters: Map<String, String>
    ): T = submitForm(baseUrl, path, parameters, formParameters).body()

    suspend fun submitForm(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: String,
        parameters: Map<String, String>,
        formParameters: Map<String, String>
    ): HttpResponse {
        return apiClient.submitForm(url = URLBuilder(baseUrl).appendPathSegments(path).apply {
            parameters.forEach { entry ->
                this.parameters[entry.key] = entry.value
            }
        }.buildString(), formParameters = Parameters.build {
            formParameters.forEach { entry ->
                this.append(entry.key, entry.value)
            }
        })
    }

    private fun insertHeadersTo(builder: HttpRequestBuilder) {
        builder.headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.UserAgent, "kSteam/1.0")
        }
    }

    @Serializable
    internal class WebApiBoxedResponse<T>(
        val response: T
    )

    private operator fun get(baseUrl: String) = WebApiOperatorScope(baseUrl = baseUrl)

    inner class WebApiOperatorScope(
        val baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
    ) {
        fun method(path: String, configurator: WebApiMethodScope.() -> Unit): WebApiMethodScope {
            return WebApiMethodScope(baseUrl, path).apply(configurator)
        }
    }

    inner class WebApiMethodScope(
        baseUrl: String,
        path: String
    ) {
        private val urlBuilder = URLBuilder(baseUrl).appendPathSegments(path)

        infix fun String.with(other: String) {
            urlBuilder.parameters.append(this, other)
        }

        infix fun String.with(other: Any) {
            urlBuilder.parameters.append(this, other.toString())
        }

        infix fun String.with(other: List<String>) {
            urlBuilder.parameters.appendAll(this, other)
        }

        suspend inline fun <reified T> body(): T = get().body<T>()
        suspend inline fun <reified T> postBody(): T = post().body<T>()

        suspend fun get(): HttpResponse {
            return apiClient.get(urlBuilder.build()) {
                insertHeadersTo(this)
            }
        }

        suspend fun post(): HttpResponse {
            return apiClient.post(urlBuilder.build()) {
                insertHeadersTo(this)
            }
        }
    }
}