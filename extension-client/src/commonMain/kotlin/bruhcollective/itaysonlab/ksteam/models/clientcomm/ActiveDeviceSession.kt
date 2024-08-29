package bruhcollective.itaysonlab.ksteam.models.clientcomm

import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.platform.Immutable

/**
 * Describes an active device running desktop Steam client.
 */
@Immutable
data class ActiveDeviceSession (
    /**
     * Instance ID that is used to send remote signals.
     */
    val id: RemoteDeviceId,

    /**
     * Protocol version of the client.
     */
    val protocolVersion: Int,

    /**
     * Name of installed OS, such as "Linux 6.x".
     */
    val osName: String,

    /**
     * Name of the device, such as "steamdeck".
     */
    val deviceName: String,

    /**
     * Type of installed OS, such as EOSType.k_Linux6x
     */
    val osType: EOSType,

    /**
     * Type of the device, such as EGamingDeviceType.k_EGamingDeviceType_SteamDeck
     */
    val deviceType: EGamingDeviceType,

    /**
     * Realm of the session - to be documented.
     */
    val realm: Int
) {
    /**
     * Returns true if the reported OS is macOS, Windows or Linux.
     */
    val probablyRunsDesktopOs: Boolean
        get() = osType in EOSType.k_WinUnknown..EOSType.k_WinMAX || osType in EOSType.k_LinuxUnknown..EOSType.k_Linux510 || osType in EOSType.k_MacOSUnknown..EOSType.k_MacOSMax

    /**
     * Returns true if the reported device type is PC, Steam Deck, Steam Machine or a handheld.
     */
    val probablyIsDesktopSteamDevice: Boolean
        get() = deviceType == EGamingDeviceType.k_EGamingDeviceType_StandardPC || deviceType == EGamingDeviceType.k_EGamingDeviceType_SteamDeck || deviceType == EGamingDeviceType.k_EGamingDeviceType_Handheld || deviceType == EGamingDeviceType.k_EGamingDeviceType_Steambox

    /**
     * Returns true if the reported device is likely to be a real desktop device that can accept commands. These checks are enough to ignore web browsers and kSteam instances running on mobile phones.
     *
     * Don't only rely on this variable as a proof that this device runs desktop Steam client - API can still throw errors even if this is true.
     */
    val probablyValidClient: Boolean
        get() = probablyRunsDesktopOs && probablyIsDesktopSteamDevice
}