package bruhcollective.itaysonlab.ksteam.network.exception

import bruhcollective.itaysonlab.ksteam.network.CMJobId

/**
 * The CM job was dropped due to timeout set by CM client.
 */
class CMJobTimeoutException(
    val id: CMJobId
): IllegalStateException("The job $id failed by a timeout.")