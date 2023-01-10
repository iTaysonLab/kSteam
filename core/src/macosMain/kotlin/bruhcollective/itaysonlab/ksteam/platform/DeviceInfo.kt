package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.cinterop.useContents
import platform.CoreFoundation.CFRelease
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSProcessInfo
import platform.SystemConfiguration.SCDynamicStoreCopyComputerName
import steam.extra.enums.EGamingDeviceType
import steam.extra.enums.EOSType
import steam.messages.auth.EAuthTokenPlatformType

internal actual fun eOSType(): EOSType {
    return NSProcessInfo.processInfo.operatingSystemVersion().useContents {
        when {
            majorVersion >= 14L -> EOSType.k_MacOSMax
            majorVersion == 13L -> EOSType.k_MacOS13
            majorVersion == 12L -> EOSType.k_MacOS12

            majorVersion == 11L -> when (minorVersion) {
                1L -> EOSType.k_MacOS111
                else -> EOSType.k_MacOS11
            }

            majorVersion == 10L -> {
                when (minorVersion) {
                    16L -> EOSType.k_MacOS1016
                    15L -> EOSType.k_MacOS1015
                    14L -> EOSType.k_MacOS1014
                    13L -> EOSType.k_MacOS1013
                    12L -> EOSType.k_MacOS1012
                    11L -> EOSType.k_MacOS1011
                    10L -> EOSType.k_MacOS1010
                    else -> EOSType.k_MacOSUnknown
                }
            }

            else -> EOSType.k_MacOSUnknown
        }
    }
}

internal actual fun eGamingDeviceType(): EGamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_StandardPC

internal actual fun deviceName(): String {
    val nameRef = SCDynamicStoreCopyComputerName(null, null)
    val actualName = CFBridgingRelease(nameRef) as? String
    CFRelease(nameRef)
    return actualName ?: "Mac"
}

internal actual fun ePlatformType(): EAuthTokenPlatformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient