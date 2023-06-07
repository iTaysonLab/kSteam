package bruhcollective.itaysonlab.ksteam.web

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import bruhcollective.itaysonlab.ksteam.web.models.GetCMListForConnectResponse
import bruhcollective.itaysonlab.ksteam.web.models.QueryTimeData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class WebApi(
    private val apiClient: HttpClient,
) {
    suspend fun getCmList(): List<CMServerEntry> {
        return ajaxGetTyped<WebApiBoxedResponse<GetCMListForConnectResponse>>(
            baseUrl = EnvironmentConstants.WEB_API_BASE,
            path = listOf("ISteamDirectory", "GetCMListForConnect", "v1"),
            parameters = mapOf(
                "cmtype" to "websockets",
                "realm" to "steamglobal",
                "maxcount" to "1"
            )
        ).response.servers
    }

    suspend fun getServerTime(): QueryTimeData {
        return apiClient.post(
            URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments(
                "ITwoFactorService",
                "QueryTime",
                "v0001"
            ).apply {
                parameters["steamid"] = "0"
            }.build()
        ).body<WebApiBoxedResponse<QueryTimeData>>().response
    }

    suspend inline fun <reified T> submitFormTyped(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: List<String>,
        parameters: Map<String, String>,
        formParameters: Map<String, String>
    ): T = submitForm(baseUrl, path, parameters, formParameters).body()

    suspend inline fun <reified T> ajaxGetTyped(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: List<String>,
        parameters: Map<String, String>,
        repeatingParameters: Map<String, List<String>> = emptyMap()
    ): T = ajaxGet(baseUrl, path, parameters, repeatingParameters).body()

    suspend fun submitForm(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: List<String>,
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

    suspend fun ajaxGet(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: List<String>,
        parameters: Map<String, String>,
        repeatingParameters: Map<String, List<String>> = emptyMap()
    ): HttpResponse {
        return apiClient.get(URLBuilder(baseUrl).appendPathSegments(path).apply {
            parameters.forEach { entry ->
                this.parameters[entry.key] = entry.value
            }

            repeatingParameters.forEach { (key, values) ->
                this.parameters.appendAll(key, values)
            }
        }.build()) {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.UserAgent, "kSteam/1.0")
            }
        }
    }

    @Serializable
    internal class WebApiBoxedResponse<T>(
        val response: T
    )

    operator fun get(path: String) = WebApiOperatorScope(path = path)

    inner class WebApiOperatorScope(
        val baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        val path: String
    ) {
        suspend inline fun <reified T> ajaxGetTyped(
            parameters: Map<String, String>,
            repeatingParameters: Map<String, List<String>> = emptyMap()
        ): T = ajaxGet(baseUrl, listOf(path), parameters, repeatingParameters).body()
    }
}