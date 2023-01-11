package bruhcollective.itaysonlab.ksteam.platform

import steam.extra.enums.EGamingDeviceType
import steam.extra.enums.EOSType
import steam.messages.auth.CAuthentication_DeviceDetails
import steam.messages.auth.EAuthTokenPlatformType

/**
 * This is used to send data to Steam when creating an authorization state.
 *
 * It will be used to show from what device you are trying to login, so it is recommended to fill it.
 */
class DeviceInformation(
    internal val osType: EOSType = EOSType.k_Windows10,
    internal val gamingDeviceType: EGamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_StandardPC,
    internal val deviceName: String = "kSteam-Client",
    internal val platformType: EAuthTokenPlatformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient,
) {
    fun toAuthDetails() = CAuthentication_DeviceDetails(
        device_friendly_name = deviceName,
        platform_type = platformType,
        os_type = osType,
        gaming_device_type = gamingDeviceType,
    )
}