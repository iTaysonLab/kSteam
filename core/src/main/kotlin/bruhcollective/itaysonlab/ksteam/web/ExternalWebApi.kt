package bruhcollective.itaysonlab.ksteam.web

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import bruhcollective.itaysonlab.ksteam.web.models.GetCMListForConnectResponse
import bruhcollective.itaysonlab.ksteam.web.models.QueryTimeData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

internal class ExternalWebApi (
    private val httpClient: HttpClient
) {
    internal suspend fun getCmList(): List<CMServerEntry> {
        return httpClient.get(URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments("ISteamDirectory", "GetCMListForConnect", "v1").apply {
            parameters["cmtype"] = "websockets"
            parameters["realm"] = "steamglobal"
            parameters["maxcount"] = "1"
        }.build()).body<WebApiBoxedResponse<GetCMListForConnectResponse>>().response.servers
    }

    internal suspend fun getServerTime(): QueryTimeData {
        return httpClient.get(URLBuilder(EnvironmentConstants.WEB_API_BASE).appendPathSegments("ITwoFactorService", "QueryTime", "v0001").apply {
            parameters["steamid"] = "0"
        }.build()).body<WebApiBoxedResponse<QueryTimeData>>().response
    }

    @Serializable
    internal class WebApiBoxedResponse <T> (
        val response: T
    )
}