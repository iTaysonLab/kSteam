package bruhcollective.itaysonlab.ksteam.network.exception

import bruhcollective.itaysonlab.ksteam.network.CMJobInformation

/**
 * The CM job was dropped.
 *
 * This exception will be thrown:
 * - if the CM job was posted after SteamClient pausing
 * - if the CM job was posted before network connection initializing
 * - if CMClient loses connection with Steam servers
 */
class CMJobDroppedException(
    info: CMJobInformation,
    reason: Reason,
): IllegalStateException("dropped $info [reason = ${reason.name}]") {
    enum class Reason {
        NetworkPaused,
        WsSessionUnavailable,
        WsConnectionDropped,
    }
}