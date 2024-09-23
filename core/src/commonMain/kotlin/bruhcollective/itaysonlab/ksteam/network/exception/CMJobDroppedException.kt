package bruhcollective.itaysonlab.ksteam.network.exception

import bruhcollective.itaysonlab.ksteam.network.CMJobId

/**
 * The CM job was dropped.
 *
 * This exception will be thrown:
 * - if the CM job was posted after SteamClient pausing
 * - if the CM job was posted before network connection initializing
 * - if CMClient loses connection with Steam servers
 */
class CMJobDroppedException(
    val id: CMJobId
): IllegalStateException("The job $id was dropped.") {
    enum class Reason {
        NetworkPaused,
        ConnectionLost,
        
    }
}