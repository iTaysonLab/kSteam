package bruhcollective.itaysonlab.ksteam.models.guard

import steam.webui.twofactor.CRemoveAuthenticatorViaChallengeContinue_Replacement_Token
import steam.webui.twofactor.CTwoFactor_AddAuthenticator_Response

fun CRemoveAuthenticatorViaChallengeContinue_Replacement_Token.toConfig(): GuardStructure {
    return GuardStructure(
        sharedSecret = shared_secret?.base64().orEmpty(),
        serialNumber = serial_number!!,
        revocationCode = revocation_code!!,
        uri = uri!!,
        serverTime = server_time!!,
        accountName = account_name!!,
        tokenGid = token_gid!!,
        identitySecret = identity_secret?.base64().orEmpty(),
        secretOne = secret_1?.base64().orEmpty(),
    )
}

fun CTwoFactor_AddAuthenticator_Response.toConfig(): GuardStructure {
    return GuardStructure(
        sharedSecret = shared_secret?.base64().orEmpty(),
        serialNumber = serial_number!!,
        revocationCode = revocation_code!!,
        uri = uri!!,
        serverTime = server_time!!,
        accountName = account_name!!,
        tokenGid = token_gid!!,
        identitySecret = identity_secret?.base64().orEmpty(),
        secretOne = secret_1?.base64().orEmpty()
    )
}
