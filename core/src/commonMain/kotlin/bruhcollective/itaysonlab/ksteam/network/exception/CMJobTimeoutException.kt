package bruhcollective.itaysonlab.ksteam.network.exception

import bruhcollective.itaysonlab.ksteam.network.CMJobInformation

/**
 * The CM job was dropped due to timeout set by CM client.
 */
class CMJobTimeoutException(
    info: CMJobInformation,
): IllegalStateException("timeout $info")