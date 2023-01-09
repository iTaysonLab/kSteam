package bruhcollective.itaysonlab.ksteam.web

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import bruhcollective.itaysonlab.ksteam.web.models.GetCMListForConnectResponse
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

    @Serializable
    internal class WebApiBoxedResponse <T> (
        val response: T
    )
}