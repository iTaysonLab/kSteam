package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import steam.extra.enums.EGamingDeviceType
import steam.extra.enums.EOSType
import steam.messages.auth.EAuthTokenPlatformType

internal actual fun eOSType(): EOSType {
    return NSProcessInfo.processInfo.operatingSystemVersion().useContents {
        when {
            majorVersion == 9L -> {
                when (minorVersion) {
                    3L -> EOSType.k_eIOS9_3
                    2L -> EOSType.k_eIOS9_2
                    1L -> EOSType.k_eIOS9_1
                    else -> EOSType.k_eIOS9
                }
            }

            majorVersion == 10L -> {
                when (minorVersion) {
                    3L -> EOSType.k_eIOS10_3
                    2L -> EOSType.k_eIOS10_2
                    1L -> EOSType.k_eIOS10_1
                    else -> EOSType.k_eIOS10
                }
            }

            majorVersion == 11L -> {
                when (minorVersion) {
                    4L -> EOSType.k_eIOS11_4
                    3L -> EOSType.k_eIOS11_3
                    2L -> EOSType.k_eIOS11_2
                    1L -> EOSType.k_eIOS11_1
                    else -> EOSType.k_eIOS11
                }
            }

            majorVersion == 12L -> {
                when (minorVersion) {
                    1L -> EOSType.k_eIOS12_1
                    else -> EOSType.k_eIOS12
                }
            }

            majorVersion >= 13L -> EOSType.k_eIOSMax

            else -> EOSType.k_eIOSUnknown
        }
    }
}

internal actual fun eGamingDeviceType(): EGamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_StandardPC

internal actual fun deviceName() = UIDevice.currentDevice.name

internal actual fun ePlatformType(): EAuthTokenPlatformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient