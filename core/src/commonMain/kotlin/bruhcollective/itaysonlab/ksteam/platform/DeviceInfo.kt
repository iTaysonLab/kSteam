package bruhcollective.itaysonlab.ksteam.platform

import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import steam.enums.EAuthTokenPlatformType
import steam.webui.authentication.CAuthentication_DeviceDetails

/**
 * This is used to send data to Steam when creating an authorization state.
 *
 * It will be used to show from what device you are trying to log in, so it is recommended to fill it.
 *
 * However, you can only fill the [deviceName] variable if you don't want to fill this.
 */
class DeviceInformation(
    /**
     * OS name and version. Visible in "All sessions" screen.
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
     * Platform type for authorizing.
     *
     * **ALWAYS USE k_EAuthTokenPlatformType_SteamClient!**
     * - k_EAuthTokenPlatformType_MobileApp does not allow you to connect to a CM (JWT does not include "client")
     * - k_EAuthTokenPlatformType_WebBrowser is unknown, but also not recommended.
     *
     * k_EAuthTokenPlatformType_MobileApp is OK if you are only using "Web" transport mode.
     * **However, you will need to re-authorize into the account in order to get an updated token with specified JWT rights.**
     */
    internal val platformType: EAuthTokenPlatformType = EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient,
) {
    fun toAuthDetails() = CAuthentication_DeviceDetails(
        device_friendly_name = deviceName,
        platform_type = platformType.ordinal,
        os_type = osType.encoded,
        gaming_device_type = gamingDeviceType.encoded,
    )
}

internal expect fun getRandomUuid(): String
internal expect fun getDefaultWorkingDirectory(): String?