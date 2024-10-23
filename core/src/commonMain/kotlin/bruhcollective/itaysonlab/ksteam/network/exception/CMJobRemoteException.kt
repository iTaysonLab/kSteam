package bruhcollective.itaysonlab.ksteam.network.exception

import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.network.CMJobInformation

/**
 * The server returned an error for the CM job.
 */
class CMJobRemoteException (
    info: CMJobInformation,
    val result: EResult
): IllegalStateException("remote $info [result = ${result.name}]")