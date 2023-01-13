package bruhcollective.itaysonlab.ksteam.platform

import steam.extra.enums.EGamingDeviceType
import steam.extra.enums.EOSType
import steam.messages.auth.CAuthentication_DeviceDetails
import steam.messages.auth.EAuthTokenPlatformType

/**
 * This is used to send data to Steam when creating an authorization state.
 *
 * It will be used to show from what device you are trying to login, so it is recommended to fill it.
 *
 * However, you can only fill the [deviceName] variable if you don't want to fill this (or don't want to add Wire as extra dependency)
 */
class DeviceInformation(
    /**
     * OS type. Visible in "All sessions" screen.
     */
    internal val osType: EOSType = EOSType.k_Windows10,
    /**
     * Gaming device type. Visible in "All sessions" screen.
     */
    internal val gamingDeviceType: EGamingDeviceType = EGamingDeviceType.k_EGamingDeviceType_StandardPC,
    /**
     * Device name. Visible on auth attempt, "All session" screen and maybe in other places.
     */
    internal val deviceName: String = "kSteam-Client",
    /**
     * Platform type. Visible in "All sessions" screen.
     */
    internal val platformType: EAuthTokenPlatformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient,
) {
    fun toAuthDetails() = CAuthentication_DeviceDetails(
        device_friendly_name = deviceName,
        platform_type = platformType,
        os_type = osType,
        gaming_device_type = gamingDeviceType,
    )
}