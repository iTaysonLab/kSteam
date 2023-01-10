package bruhcollective.itaysonlab.ksteam.platform

import steam.extra.enums.EGamingDeviceType
import steam.extra.enums.EOSType
import steam.messages.auth.EAuthTokenPlatformType

internal expect fun eOSType(): EOSType
internal expect fun eGamingDeviceType(): EGamingDeviceType
internal expect fun deviceName(): String
internal expect fun ePlatformType(): EAuthTokenPlatformType