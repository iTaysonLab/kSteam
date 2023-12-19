package bruhcollective.itaysonlab.ksteam.models.account

import bruhcollective.itaysonlab.ksteam.models.SteamId
import steam.enums.EAuthSessionGuardType
import steam.webui.authentication.CAuthentication_BeginAuthSessionViaCredentials_Response

sealed interface AuthorizationState {
    data object Unauthorized : AuthorizationState

    data object Success : AuthorizationState

    class AwaitingTwoFactor internal constructor(
        val steamId: SteamId,
        val supportedConfirmationMethods: List<ConfirmationMethod>,
        internal val sumProtos: List<EAuthSessionGuardType>
    ) : AuthorizationState {
        internal constructor(networkResponse: CAuthentication_BeginAuthSessionViaCredentials_Response) : this(
            supportedConfirmationMethods = networkResponse.allowed_confirmations.mapNotNull {
                when (EAuthSessionGuardType.fromValue(it.confirmation_type ?: 0)) {
                    EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode -> ConfirmationMethod.EmailCode
                    EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode -> ConfirmationMethod.DeviceCode
                    EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation -> ConfirmationMethod.DeviceConfirmation
                    EAuthSessionGuardType.k_EAuthSessionGuardType_EmailConfirmation -> ConfirmationMethod.EmailConfirmation
                    EAuthSessionGuardType.k_EAuthSessionGuardType_MachineToken -> ConfirmationMethod.MachineToken
                    else -> null
                }
            },
            sumProtos = networkResponse.allowed_confirmations.mapNotNull { EAuthSessionGuardType.fromValue(it.confirmation_type ?: Int.MAX_VALUE) },
            steamId = SteamId((networkResponse.steamid ?: 0L).toULong())
        )

        enum class ConfirmationMethod {
            // Manual: you need to enter a code from the email
            EmailCode,

            // Manual: you need to enter a code from any Steam-supporting authenticator app
            DeviceCode,

            // Automatic: you need to confirm a request from any app (Steam Mobile, Jetisteam etc)
            DeviceConfirmation,

            // Automatic: you need to click a link in a email
            EmailConfirmation,

            // Fully-Automatic: use an existing SG auth data?
            MachineToken
        }
    }
}