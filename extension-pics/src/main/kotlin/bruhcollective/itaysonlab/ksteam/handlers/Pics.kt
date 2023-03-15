package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.keyvalue.PicsVdfKvDatabase
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.library.DynamicFilters
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import bruhcollective.itaysonlab.ksteam.platform.dispatchListProcessing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import steam.webui.common.*

/**
 * A handler to access the PICS infrastructure
 */
class Pics internal constructor(
    private val steamClient: SteamClient,
    private val database: PicsVdfKvDatabase
) : BaseHandler {
    private val _isPicsAvailable = MutableStateFlow(PicsState.Initialization)
    val isPicsAvailable = _isPicsAvailable.asStateFlow()

    suspend fun getAppIdsAsInfos(appIds: List<AppId>, limit: Int = 0): List<AppInfo> = appIds.mapNotNull { appId ->
        database.vdf.apps.get(appId)
    }

    private suspend fun getAppIdsFiltered(filters: DynamicFilters, limit: Int = 0): List<AppInfo> = database.vdf.sortAppsByDynamicFilters(filters).toList()

    internal suspend fun getAppSummariesFiltered(filters: DynamicFilters, limit: Int = 0): List<AppSummary> = getAppIdsFiltered(filters, limit).map { app ->
        AppSummary(AppId(app.appId), app.common.name)
    }

    suspend fun getAppSummariesByAppId(appIds: List<AppId>, limit: Int = 0) = getAppIdsAsInfos(appIds).associate { app ->
        AppId(app.appId) to AppSummary(AppId(app.appId), app.common.name)
    }

    suspend fun getAppInfo(id: AppId): AppInfo? = database.vdf.apps.get(id)

    // region Internal stuff

    private var processedLicenses = mutableListOf<CMsgClientLicenseList_License>()

    // TODO: filter based on owning package ids
    internal var appIds: Sequence<AppId> = emptySequence()
        private set

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

        database.vdf.packages.initialize()
        database.vdf.apps.initialize()

        // TODO: get latest update number and compare changes

        val requiresUpdate = licenses.filter { sLicense ->
            if (sLicense.package_id != null && sLicense.change_number != null) {
                database.vdf.packages.get(sLicense.package_id!!).let { dLicense ->
                    dLicense == null || sLicense.change_number!! > database.vdf.getChangeNumberFor(PicsVdfKvDatabase.Keys.Packages, sLicense.package_id!!)
                }
            } else {
                false
            }
        }

        logVerbose("Pics:HandleLicenses", "Require update: ${requiresUpdate.size}")

        if (requiresUpdate.isNotEmpty()) {
            requestPicsMetadataForLicenses(requiresUpdate)
        }

        // TODO: check integrity and update if not all ids are present

        appIds = database.vdf.apps.getKeys()

        _isPicsAvailable.value = PicsState.Ready
    }

    private suspend fun requestPicsMetadataForLicenses(requiresUpdate: List<CMsgClientLicenseList_License>) {
        _isPicsAvailable.value = PicsState.UpdatingPackages

        val appIds = dispatchListProcessing(loadPackageInfo(requiresUpdate).distinctBy { it.packageid }) { pkgInfo ->
            val packageId = pkgInfo.packageid ?: return@dispatchListProcessing null
            val changeNumber = pkgInfo.change_number?.toUInt()?.toLong() ?: return@dispatchListProcessing null
            val buffer = pkgInfo.buffer?.toByteArray() ?: return@dispatchListProcessing null

            database.vdf.parseBinaryVdf<PackageInfo>(buffer)?.also {
                database.vdf.putPicsMetadata(PicsVdfKvDatabase.Keys.Packages, packageId, changeNumber, buffer)
            }?.appIds
        }.flatten()

        // We will just assume that any changed packages means that all games are updated
        // However, we should use saved access tokens (and check appids in future)

        _isPicsAvailable.value = PicsState.UpdatingApps

        dispatchListProcessing(loadAppsInfo(appIds).distinctBy { it.appid }) { appInfo ->
            val appId = appInfo.appid ?: return@dispatchListProcessing null
            val changeNumber = appInfo.change_number?.toUInt()?.toLong() ?: return@dispatchListProcessing null
            val buffer = appInfo.buffer?.toByteArray() ?: return@dispatchListProcessing null

            database.vdf.parseTextVdf<AppInfo>(buffer)?.also {
                database.vdf.putPicsMetadata(PicsVdfKvDatabase.Keys.Apps, appId, changeNumber, buffer)
            }
        }
    }

    private suspend fun loadPackageInfo(licenses: List<CMsgClientLicenseList_License>): List<CMsgClientPICSProductInfoResponse_PackageInfo> {
        return requestDataFromPics(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                packages = licenses.map {
                    CMsgClientPICSProductInfoRequest_PackageInfo(
                        packageid = it.package_id,
                        access_token = it.access_token,
                    )
                }
            ), emitter = {
                it.packages
            }
        )
    }

    private suspend fun loadAppsInfo(appIds: List<Int>): List<CMsgClientPICSProductInfoResponse_AppInfo> {
        val tokens = steamClient.execute(SteamPacket.newProto(
            messageId = EMsg.k_EMsgClientPICSAccessTokenRequest,
            adapter = CMsgClientPICSAccessTokenRequest.ADAPTER,
            payload = CMsgClientPICSAccessTokenRequest(appids = appIds)
        )).getProtoPayload(CMsgClientPICSAccessTokenResponse.ADAPTER).data.app_access_tokens

        return requestDataFromPics(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                apps = tokens.mapNotNull {
                    CMsgClientPICSProductInfoRequest_AppInfo(
                        appid = it.appid ?: return@mapNotNull null,
                        access_token = it.access_token ?: 0,
                    )
                }
            ), emitter = {
                it.apps
            }
        )
    }

    @OptIn(FlowPreview::class)
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

    // endregion

    enum class PicsState {
        // 1. Awaiting information from the server
        Initialization,
        // 2. Updating PICS information - Packages
        UpdatingPackages,
        // 2. Updating PICS information - APps
        UpdatingApps,
        // 3. PICS is ready to use
        Ready
    }
}