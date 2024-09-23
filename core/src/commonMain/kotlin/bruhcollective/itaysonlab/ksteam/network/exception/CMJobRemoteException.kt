package bruhcollective.itaysonlab.ksteam.network.exception

import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.network.CMJobId

/**
 * The server returned an error for the CM job.
 */
class CMJobRemoteException (
    val id: CMJobId,
    val result: EResult
): IllegalStateException("The job $id failed with a result: $result")