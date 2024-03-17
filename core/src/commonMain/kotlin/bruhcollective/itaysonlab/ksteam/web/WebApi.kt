package bruhcollective.itaysonlab.ksteam.web

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import bruhcollective.itaysonlab.ksteam.web.models.GetCMListForConnectResponse
import bruhcollective.itaysonlab.ksteam.web.models.QueryTimeData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.Serializable

class WebApi(
    private val apiClient: HttpClient,
) {
    private companion object {
        const val LOG_TAG = "Core:WebApi"
    }

    val gateway = this[EnvironmentConstants.WEB_API_BASE]
    val community = this[EnvironmentConstants.COMMUNITY_API_BASE]
    val store = this[EnvironmentConstants.STORE_API_BASE]

    suspend fun getCmList(): List<CMServerEntry> {
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

    suspend fun submitProtobufForm(
        path: String,
        data: String
    ): HttpResponse {
        return apiClient.submitFormWithBinaryData(
            url = URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments(path).buildString(), formData = listOf(
                PartData.FormItem(data, {}, Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"input_protobuf_encoded\"")
                    append(HttpHeaders.ContentLength, data.length.toString())
                })
            )
        )
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
        fun method(path: String, configurator: WebApiMethodScope.() -> Unit = {}): WebApiMethodScope {
            return WebApiMethodScope(baseUrl, path).apply(configurator)
        }
    }

    inner class WebApiMethodScope(
        baseUrl: String,
        path: String
    ) {
        private val urlBuilder = URLBuilder(baseUrl).appendPathSegments(path)

        infix fun String.with(other: String?) {
            if (other != null) {
                urlBuilder.parameters.append(this, other)
            }
        }

        infix fun String.with(other: Any?) {
            if (other != null) {
                urlBuilder.parameters.append(this, other.toString())
            }
        }

        infix fun String.with(other: List<String>) {
            urlBuilder.parameters.appendAll(this, other)
        }

        suspend inline fun <reified T> body(): T = get().body<T>()
        suspend inline fun <reified T> postBody(): T = post().body<T>()

        suspend fun get(): HttpResponse {
            KSteamLogging.logVerbose(LOG_TAG) {
                "[get] ${urlBuilder.build()}"
            }

            return apiClient.get(urlBuilder.build()) {
                insertHeadersTo(this)
            }
        }

        suspend fun post(): HttpResponse {
            KSteamLogging.logVerbose(LOG_TAG) {
                "[post] ${urlBuilder.build()}"
            }

            return apiClient.post(urlBuilder.build()) {
                insertHeadersTo(this)
            }
        }
    }
}