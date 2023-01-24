package bruhcollective.itaysonlab.ksteam.web

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.guard.models.ConfirmationListState
import bruhcollective.itaysonlab.ksteam.guard.models.MobileConfResult
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import bruhcollective.itaysonlab.ksteam.web.models.GetCMListForConnectResponse
import bruhcollective.itaysonlab.ksteam.web.models.QueryTimeData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeStart_Response

internal class ExternalWebApi(
    private val httpClient: HttpClient
) {
    internal suspend fun getCmList(): List<CMServerEntry> {
        return httpClient.get(
            URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments(
                "ISteamDirectory",
                "GetCMListForConnect",
                "v1"
            ).apply {
                parameters["cmtype"] = "websockets"
                parameters["realm"] = "steamglobal"
                parameters["maxcount"] = "1"
            }.build()
        ).body<WebApiBoxedResponse<GetCMListForConnectResponse>>().response.servers
    }

    internal suspend fun getServerTime(): QueryTimeData {
        return httpClient.post(
            URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments(
                "ITwoFactorService",
                "QueryTime",
                "v0001"
            ).apply {
                parameters["steamid"] = "0"
            }.build()
        ).body<WebApiBoxedResponse<QueryTimeData>>().response
    }

    internal suspend fun requestMove(accessToken: String): CTwoFactor_RemoveAuthenticatorViaChallengeStart_Response {
        return httpClient.post(
            URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments(
                "ITwoFactorService",
                "RemoveAuthenticatorViaChallengeStart",
                "v1"
            ).apply {
                parameters["access_token"] = accessToken
            }.build()
        ).readBytes().let { CTwoFactor_RemoveAuthenticatorViaChallengeStart_Response.ADAPTER.decode(it) }
    }

    private val json = Json {
        classDiscriminator = "success"
    }

    internal suspend fun getConfirmations(
        platform: String,
        steamId: Long,
        timestamp: Long,
        signature: String,
        m: String = "react",
        tag: String = "list"
    ): ConfirmationListState {
        return httpClient.get(
            URLBuilder(EnvironmentConstants.COMMUNITY_API_BASE).appendPathSegments(
                "mobileconf",
                "getlist"
            ).apply {
                parameters["p"] = platform
                parameters["a"] = steamId.toString()
                parameters["t"] = timestamp.toString()
                parameters["k"] = signature
                parameters["m"] = m
                parameters["tag"] = tag
            }.build()
        ).bodyAsText().let {
            ConfirmationListState.Decoder.decodeFromString(it)
        }
    }

    internal suspend fun runConfOperation(
        platform: String,
        steamId: Long,
        timestamp: Long,
        m: String = "react",
        tag: String, // reject/accept
        operation: String, // cancel/allow
        cid: String,
        ck: String,
        signature: String,
    ): MobileConfResult {
        return httpClient.get(
            URLBuilder(EnvironmentConstants.COMMUNITY_API_BASE).appendPathSegments(
                "mobileconf",
                "ajaxop"
            ).apply {
                parameters["p"] = platform
                parameters["a"] = steamId.toString()
                parameters["t"] = timestamp.toString()
                parameters["k"] = signature
                parameters["m"] = m
                parameters["tag"] = tag
                parameters["op"] = operation
                parameters["cid"] = cid
                parameters["ck"] = ck
            }.build()
        ).body()
    }

    @Serializable
    internal class WebApiBoxedResponse<T>(
        val response: T
    )
}