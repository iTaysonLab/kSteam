package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.web.WebApi
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry

/**
 * Manages a list of CM servers.
 */
internal class CMList(
    private val webApi: WebApi,
    private val logger: Logger
) {
    private val wsEndpoints = mutableListOf<String>()
    private var wsEndpointSelected: String = ""
    private var wsEndpointIterator: Iterator<String> = wsEndpoints.iterator()

    /**
     * Returns if this server list is empty.
     */
    fun isEmpty(): Boolean {
        return wsEndpoints.isEmpty()
    }

    /**
     * Returns if this server list is not empty.
     */
    fun isNotEmpty(): Boolean {
        return wsEndpoints.isNotEmpty()
    }

    /**
     * Returns current CM endpoint for use.
     */
    fun getEndpoint(): String {
        require(wsEndpointSelected.isNotEmpty()) { "CMList was not initialized. Call refreshServerList before attempting to connect." }
        return wsEndpointSelected
    }

    /**
     * Marks current CM endpoint as bad. Next [getEndpoint] call will return the next endpoint in the list.
     */
    fun markEndpointAsBad() {
        logger.logWarning("CMList") { "Marking $wsEndpointSelected as bad." }

        if (wsEndpointIterator.hasNext()) {
            wsEndpointSelected = wsEndpointIterator.next()
            logger.logWarning("CMList") { "The next endpoint will be $wsEndpointSelected." }
        }
    }

    /**
     * Loads the list of CM servers from the network.
     */
    suspend fun refreshServerList() {
        webApi.getCmList().also { entries ->
            wsEndpoints.clear()
            wsEndpoints.addAll(entries.map(CMServerEntry::endpoint))
            wsEndpointSelected = wsEndpoints.first()
            wsEndpointIterator = wsEndpoints.iterator()
        }
    }
}