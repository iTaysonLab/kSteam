package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.web.ExternalWebApi
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry

internal class CMList (
    private val externalWebApi: ExternalWebApi
) {
    private val servers = mutableListOf<CMServerEntry>()

    suspend fun getBestServer(): CMServerEntry {
        if (servers.isEmpty()) {
            servers.addAll(externalWebApi.getCmList())
        }

        return servers.first()
    }
}