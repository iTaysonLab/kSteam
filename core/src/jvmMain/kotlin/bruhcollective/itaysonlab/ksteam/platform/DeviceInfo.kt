package bruhcollective.itaysonlab.ksteam.platform

import steam.extra.enums.EGamingDeviceType
import steam.extra.enums.EOSType
import steam.messages.auth.EAuthTokenPlatformType

// TODO: Actual detection
internal actual fun eOSType(): EOSType = EOSType.k_Windows10

internal actual fun eGamingDeviceType(): EGamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_StandardPC

internal actual fun deviceName(): String {
    return System.getenv().let { env ->
        env["COMPUTERNAME"] ?: env["HOSTNAME"] ?: "Unknown Desktop"
    }
}

internal actual fun ePlatformType() = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient