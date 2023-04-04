package bruhcollective.itaysonlab.ksteam.guard.models

import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.util.ipString
import steam.webui.authentication.CAuthentication_RefreshToken_Enumerate_Response_RefreshTokenDescription
import steam.webui.authentication.CAuthentication_RefreshToken_Enumerate_Response_TokenUsageEvent
import steam.webui.authentication.EAuthSessionGuardType
import steam.webui.authentication.EAuthTokenPlatformType
import steam.webui.common.CMsgIPAddress

/**
 * Represents a session which is approved to access Steam.
 */
data class ActiveSession internal constructor(
    val id: Long,
    val deviceName: String,
    val timeUpdated: Int,
    val platformType: EAuthTokenPlatformType,
    val loggedIn: Boolean,
    val osPlatform: Int, // TODO
    val confirmedWith: EAuthSessionGuardType,
    val gamingDeviceType: EGamingDeviceType,
    val firstSeen: UsageData?,
    val lastSeen: UsageData?,
    val osType: EOSType,
    private val _proto: CAuthentication_RefreshToken_Enumerate_Response_RefreshTokenDescription
) {
    /**
     * Get a [ByteString] of the CAuthentication_RefreshToken_Enumerate_Response_RefreshTokenDescription protobuf.
     *
     * Useful for using as a argument in Android Navigation
     *
     * TODO: in Jetisteam, reference to a session by it's ID, so this function can be safely removed
     */
    fun protoBytes() = _proto.encodeByteString()

    data class UsageData internal constructor(
        val time: Int,
        val ip: CMsgIPAddress,
        val locale: String,
        val country: String,
        val state: String,
        val city: String
    ) {
        val ipString = ip.ipString

        internal constructor(proto: CAuthentication_RefreshToken_Enumerate_Response_TokenUsageEvent) : this(
            time = proto.time ?: 0,
            ip = proto.ip ?: CMsgIPAddress(v4 = 0),
            locale = proto.locale.orEmpty(),
            country = proto.country.orEmpty(),
            state = proto.state.orEmpty(),
            city = proto.city.orEmpty()
        )
    }

    constructor(proto: CAuthentication_RefreshToken_Enumerate_Response_RefreshTokenDescription) : this(
        id = proto.token_id ?: 0L,
        deviceName = proto.token_description.orEmpty(),
        timeUpdated = proto.time_updated ?: 0,
        platformType = proto.platform_type ?: EAuthTokenPlatformType.k_EAuthTokenPlatformType_Unknown,
        loggedIn = proto.logged_in ?: false,
        osPlatform = proto.os_platform ?: 0,
        confirmedWith = proto.auth_type?.let { EAuthSessionGuardType.fromValue(it) }
            ?: EAuthSessionGuardType.k_EAuthSessionGuardType_Unknown,
        gamingDeviceType = EGamingDeviceType.byEncoded(proto.gaming_device_type),
        firstSeen = proto.first_seen?.let { UsageData(it) },
        lastSeen = proto.last_seen?.let { UsageData(it) },
        osType = EOSType.byEncoded(proto.os_type),
        _proto = proto
    )
}