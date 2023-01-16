package bruhcollective.itaysonlab.ksteam.models.account

import bruhcollective.itaysonlab.ksteam.models.SteamId
import steam.messages.auth.CAuthentication_BeginAuthSessionViaCredentials_Response
import steam.messages.auth.EAuthSessionGuardType

sealed class AuthorizationState {
    object Unauthorized: AuthorizationState()

    object Success: AuthorizationState()

    class AwaitingTwoFactor(
        val steamId: SteamId,
        val supportedConfirmationMethods: List<ConfirmationMethod>,
        internal val sumProtos: List<EAuthSessionGuardType>
    ): AuthorizationState() {
        internal constructor(networkResponse: CAuthentication_BeginAuthSessionViaCredentials_Response): this(
            supportedConfirmationMethods = networkResponse.allowed_confirmations.mapNotNull {
                when (it.confirmation_type) {
                    EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode -> ConfirmationMethod.EmailCode
                    EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode -> ConfirmationMethod.DeviceCode
                    EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation -> ConfirmationMethod.DeviceConfirmation
                    EAuthSessionGuardType.k_EAuthSessionGuardType_EmailConfirmation -> ConfirmationMethod.EmailConfirmation
                    EAuthSessionGuardType.k_EAuthSessionGuardType_MachineToken -> ConfirmationMethod.MachineToken
                    else -> null
                }
            }, sumProtos = networkResponse.allowed_confirmations.mapNotNull { it.confirmation_type }, steamId = SteamId((networkResponse.steamid ?: 0L).toULong())
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
            // Fully-Automatic: use a existing SG auth data?
            MachineToken
        }
    }
}