package bruhcollective.itaysonlab.ksteam.guard.models

import androidx.compose.runtime.Stable
import steam.webui.authentication.CAuthentication_GetAuthSessionInfo_Response
import steam.webui.authentication.EAuthSessionSecurityHistory
import steam.webui.authentication.EAuthTokenPlatformType
import steam.webui.authentication.ESessionPersistence

/**
 * Represents a session which is waiting to be confirmed/rejected.
 */
@Stable
data class AwaitingSession(
    val id: Long,
    val ip: String,
    val geoloc: String,
    val city: String,
    val state: String,
    val country: String,
    val platformType: EAuthTokenPlatformType,
    val deviceName: String,
    val version: Int,
    val loginHistory: EAuthSessionSecurityHistory,
    val isLocationMismatch: Boolean,
    val isHighUsageLogin: Boolean,
    val requestedPersistedSession: Boolean
) {
    constructor(id: Long, proto: CAuthentication_GetAuthSessionInfo_Response) : this(
        id = id,
        ip = proto.ip.orEmpty(),
        geoloc = proto.geoloc.orEmpty(),
        city = proto.city.orEmpty(),
        state = proto.state.orEmpty(),
        country = proto.country.orEmpty(),
        platformType = proto.platform_type ?: EAuthTokenPlatformType.k_EAuthTokenPlatformType_Unknown,
        deviceName = proto.device_friendly_name.orEmpty(),
        version = proto.version ?: 1,
        loginHistory = proto.login_history ?: EAuthSessionSecurityHistory.k_EAuthSessionSecurityHistory_Invalid,
        isLocationMismatch = proto.requestor_location_mismatch ?: false,
        isHighUsageLogin = proto.high_usage_login ?: false,
        requestedPersistedSession = proto.requested_persistence == ESessionPersistence.k_ESessionPersistence_Persistent
    )
}