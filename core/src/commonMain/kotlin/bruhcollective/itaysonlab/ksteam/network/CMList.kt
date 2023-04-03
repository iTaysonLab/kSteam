package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.web.WebApi
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry

internal class CMList(
    private val webApi: WebApi
) {
    private val servers = mutableListOf<CMServerEntry>()

    suspend fun getBestServer(): CMServerEntry {
        if (servers.isEmpty()) {
            servers.addAll(webApi.getCmList())
        }

        return servers.first()
    }
}