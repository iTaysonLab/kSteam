package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.web.WebApi
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request
import steam.webui.twofactor.CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response

internal suspend fun WebApi.guardMoveStart(accessToken: String) {
    submitForm(
        baseUrl = EnvironmentConstants.WEB_API_BASE,
        path = listOf("ITwoFactorService", "RemoveAuthenticatorViaChallengeStart", "v1"),
        parameters = mapOf("access_token" to accessToken),
        formParameters = emptyMap()
    )
}

internal suspend fun WebApi.guardMoveConfirm(accessToken: String, obj: CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Request): CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response? {
    return try {
        submitFormTyped<ByteArray>(
            baseUrl = EnvironmentConstants.WEB_API_BASE,
            path = listOf("ITwoFactorService", "RemoveAuthenticatorViaChallengeContinue", "v1"),
            parameters = mapOf("access_token" to accessToken),
            formParameters = mapOf("input_protobuf_encoded" to obj.encodeByteString().base64().dropLast(1))
        ).let { response ->
            CTwoFactor_RemoveAuthenticatorViaChallengeContinue_Response.ADAPTER.decode(response)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}