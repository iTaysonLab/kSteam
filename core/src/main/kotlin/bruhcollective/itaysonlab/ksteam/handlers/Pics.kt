package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.pics.PicsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import steam.webui.common.*

/**
 * A handler to access the PICS infrastructure
 */
class Pics(
    private val steamClient: SteamClient
) : BaseHandler {
    private var processedLicenses = mutableListOf<CMsgClientLicenseList_License>()

    private val database = PicsDatabase(steamClient)

    /**
     * 1: Receive k_EMsgClientLicenseList
     * 2: Collect package IDs from CMsgClientLicenseList
     * 3: Request not cached package IDs from PICS
     * 4: Parse binary VDFs from PICS and collect app IDs
     * 5: Request appinfos from PICS, parse text VDFs
     * Bonus: collect already cached appinfos, get change number, ask PICS for changes and then refresh the outdated app/packages!
     */
    override suspend fun onEvent(packet: SteamPacket) {
        if (packet.messageId == EMsg.k_EMsgClientLicenseList) {
            handleServerLicenseList(packet.getProtoPayload(CMsgClientLicenseList.ADAPTER).data.licenses)
        }
    }

    private suspend fun handleServerLicenseList(licenses: List<CMsgClientLicenseList_License>) {
        logVerbose("Pics:HandleLicenses", "Got licenses: ${licenses.size}")
        processedLicenses += licenses

        val allDatabasePackages = withContext(Dispatchers.IO) {
            database.picsEntryQueries.selectEverything("package").executeAsList().associateBy { it.id }
        }

        val requiresUpdate = licenses.filter { sLicense ->
            allDatabasePackages[sLicense.package_id!!.toLong()].let { dLicense ->
                dLicense == null || sLicense.change_number!!.toLong() > dLicense.changeNumber || sLicense.access_token != dLicense.accessToken
            }
        }

        loadPackageInfo(requiresUpdate)
    }

    private suspend fun loadPackageInfo(licenses: List<CMsgClientLicenseList_License>) {
        requestDataFromPics(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                packages = licenses.map {
                    CMsgClientPICSProductInfoRequest_PackageInfo(
                        packageid = it.package_id,
                        access_token = it.access_token
                    )
                }
            ), emitter = {
                it.packages
            }
        )
    }

    private suspend fun <T> requestDataFromPics(request: CMsgClientPICSProductInfoRequest, emitter: (CMsgClientPICSProductInfoResponse) -> List<T>): List<T> {
        return steamClient.subscribe(SteamPacket.newProto(
            messageId = EMsg.k_EMsgClientPICSProductInfoRequest,
            adapter = CMsgClientPICSProductInfoRequest.ADAPTER,
            payload = request
        )).transformWhile { packet ->
            packet.getProtoPayload(CMsgClientPICSProductInfoResponse.ADAPTER).data.let { response ->
                emit(response.let(emitter))
                response.response_pending ?: false
            }
        }.flatMapConcat { it.asFlow() }.toList()
    }
}