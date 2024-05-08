package bruhcollective.itaysonlab.ksteam.guard.models

import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import bruhcollective.itaysonlab.ksteam.util.ipString
import kotlinx.serialization.Serializable
import steam.enums.EAuthSessionGuardType
import steam.enums.EAuthTokenPlatformType
import steam.webui.authentication.CAuthentication_RefreshToken_Enumerate_Response_RefreshTokenDescription
import steam.webui.authentication.CAuthentication_RefreshToken_Enumerate_Response_TokenUsageEvent

/**
 * Represents a session which is approved to access Steam.
 */
@Immutable
@Serializable
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
    val isCurrentSession: Boolean
) {
    @Immutable
    @Serializable
    data class UsageData internal constructor(
        val time: Int,
        val ip: String,
        val locale: String,
        val country: String,
        val state: String,
        val city: String
    ) {
        internal constructor(proto: CAuthentication_RefreshToken_Enumerate_Response_TokenUsageEvent) : this(
            time = proto.time ?: 0,
            ip = proto.ip?.ipString.orEmpty(),
            locale = proto.locale.orEmpty(),
            country = proto.country.orEmpty(),
            state = proto.state.orEmpty(),
            city = proto.city.orEmpty()
        )
    }

    constructor(proto: CAuthentication_RefreshToken_Enumerate_Response_RefreshTokenDescription, isCurrentSession: Boolean = false) : this(
        id = proto.token_id ?: 0L,
        deviceName = proto.token_description.orEmpty(),
        timeUpdated = proto.time_updated ?: 0,
        platformType = EAuthTokenPlatformType.fromValue(proto.platform_type ?: 0) ?: EAuthTokenPlatformType.k_EAuthTokenPlatformType_Unknown,
        loggedIn = proto.logged_in ?: false,
        osPlatform = proto.os_platform ?: 0,
        confirmedWith = proto.auth_type?.let { EAuthSessionGuardType.fromValue(it) }
            ?: EAuthSessionGuardType.k_EAuthSessionGuardType_Unknown,
        gamingDeviceType = EGamingDeviceType.byEncoded(proto.gaming_device_type),
        firstSeen = proto.first_seen?.let { UsageData(it) },
        lastSeen = proto.last_seen?.let { UsageData(it) },
        osType = EOSType.byEncoded(proto.os_type),
        isCurrentSession = isCurrentSession
    )
}