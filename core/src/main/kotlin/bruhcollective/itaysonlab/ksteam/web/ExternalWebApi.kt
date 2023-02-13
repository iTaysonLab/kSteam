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
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response

internal class ExternalWebApi(
    private val apiClient: HttpClient,
) {
    internal suspend fun getCmList(): List<CMServerEntry> {
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

    internal suspend fun getServerTime(): QueryTimeData {
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

    internal suspend fun guardMoveStart(accessToken: String) {
        apiClient.post(
            URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments(
                "ITwoFactorService",
                "RemoveAuthenticatorViaChallengeStart",
                "v1"
            ).apply {
                parameters["access_token"] = accessToken
            }.build()
        )
    }

    internal suspend fun guardMoveConfirm(accessToken: String, obj: CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request): CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response? {
        return try {
            apiClient.submitForm(url = EnvironmentConstants.WEB_API_BASE, formParameters = Parameters.build {
                append("input_protobuf_encoded", obj.encodeByteString().base64().dropLast(1))
            }) {
                url {
                    appendPathSegments("ITwoFactorService", "RemoveAuthenticatorViaChallengeContinue", "v1")
                    parameters.append("access_token", accessToken)
                }
            }.body<ByteArray>().let {
                CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response.ADAPTER.decode(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    internal suspend inline fun <reified T> ajaxGetTyped(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: List<String>,
        parameters: Map<String, String>
    ): T = ajaxGet(baseUrl, path, parameters).body()

    internal suspend fun ajaxGet(
        baseUrl: String = EnvironmentConstants.COMMUNITY_API_BASE,
        path: List<String>,
        parameters: Map<String, String>
    ): HttpResponse {
        return apiClient.get(URLBuilder(baseUrl).appendPathSegments(path).apply {
            parameters.forEach { entry ->
                this.parameters[entry.key] = entry.value
            }
        }.build())
    }

    @Serializable
    internal class WebApiBoxedResponse<T>(
        val response: T
    )
}