package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.web.WebApi
import io.ktor.client.call.*
import io.ktor.client.statement.*
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response

internal suspend fun WebApi.guardMoveStart(accessToken: String) {
    try {
        submitForm(
            baseUrl = EnvironmentConstants.WEB_API_BASE,
            path = "ITwoFactorService/RemoveAuthenticatorViaChallengeStart/v1",
            parameters = mapOf("access_token" to accessToken),
            formParameters = emptyMap()
        ).bodyAsText() // explicitly close body
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal suspend fun WebApi.guardMoveConfirm(accessToken: String, obj: CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request): CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response? {
    return try {
        val formResponse = submitForm(
            baseUrl = EnvironmentConstants.WEB_API_BASE,
            path = "ITwoFactorService/RemoveAuthenticatorViaChallengeContinue/v1",
            parameters = mapOf("access_token" to accessToken),
            formParameters = mapOf("input_protobuf_encoded" to obj.encodeByteString().base64().dropLast(1))
        ).body<ByteArray>()

        CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response.ADAPTER.decode(formResponse)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}