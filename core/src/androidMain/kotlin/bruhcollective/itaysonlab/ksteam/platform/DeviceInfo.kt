package bruhcollective.itaysonlab.ksteam.platform

import android.os.Build
import android.provider.Settings
import steam.extra.enums.EGamingDeviceType
import steam.extra.enums.EOSType
import steam.messages.auth.EAuthTokenPlatformType

internal actual fun eOSType(): EOSType = when (Build.VERSION.SDK_INT) {
    Build.VERSION_CODES.M -> EOSType.k_eAndroid6
    Build.VERSION_CODES.N, Build.VERSION_CODES.N_MR1 -> EOSType.k_eAndroid7
    Build.VERSION_CODES.O, Build.VERSION_CODES.O_MR1 -> EOSType.k_eAndroid8
    Build.VERSION_CODES.P -> EOSType.k_eAndroid9
    else -> EOSType.k_eAndroidUnknown
}

internal actual fun eGamingDeviceType(): EGamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_Phone

internal actual fun deviceName() = Build.MODEL

internal actual fun ePlatformType() = EAuthTokenPlatformType.k_EAuthTokenPlatformType_MobileApp