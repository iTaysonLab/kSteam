package bruhcollective.itaysonlab.ksteam.guard.models

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.proto.GuardConfiguration
import steam.webui.twofactor.CRemoveAuthenticatorViaChallengeContinue_Replacement_Token
import steam.webui.twofactor.CTwoFactor_AddAuthenticator_Response

fun CRemoveAuthenticatorViaChallengeContinue_Replacement_Token.toConfig(): GuardConfiguration {
    return GuardConfiguration(
        shared_secret = shared_secret!!,
        serial_number = serial_number!!,
        revocation_code = revocation_code!!,
        uri = uri!!,
        server_time = server_time!!,
        account_name = account_name!!,
        token_gid = token_gid!!,
        identity_secret = identity_secret!!,
        secret_1 = secret_1!!,
        steam_id = steamid!!,
    )
}

fun CTwoFactor_AddAuthenticator_Response.toConfig(steamId: SteamId): GuardConfiguration {
    return GuardConfiguration(
        shared_secret = shared_secret!!,
        serial_number = serial_number!!,
        revocation_code = revocation_code!!,
        uri = uri!!,
        server_time = server_time!!,
        account_name = account_name!!,
        token_gid = token_gid!!,
        identity_secret = identity_secret!!,
        secret_1 = secret_1!!,
        steam_id = steamId.longId
    )
}
