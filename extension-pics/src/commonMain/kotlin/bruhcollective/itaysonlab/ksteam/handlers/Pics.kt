package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.keyvalue.PicsVdfKvDatabase
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.extension.plugins.MetadataPlugin
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
) : BaseHandler, MetadataPlugin {
    private val _isPicsAvailable = MutableStateFlow(PicsState.Initialization)
    val isPicsAvailable = _isPicsAvailable.asStateFlow()

    suspend fun getAppIdsAsInfos(appIds: List<AppId>): List<AppInfo> = appIds.mapNotNull { appId ->
        database.apps.get(appId.id)
    }

    private suspend fun getAppIdsFiltered(filters: DynamicFilters): Sequence<AppInfo> = database.sortAppsByDynamicFilters(filters)

    internal suspend fun getAppSummariesFiltered(filters: DynamicFilters): Sequence<AppSummary> = getAppIdsFiltered(filters).map { app ->
        AppSummary(AppId(app.appId), app.common.name)
    }

    suspend fun getAppSummariesByAppId(appIds: List<AppId>) = getAppIdsAsInfos(appIds).associate { app ->
        AppId(app.appId) to AppSummary(AppId(app.appId), app.common.name)
    }

    suspend fun getAppInfo(id: AppId): AppInfo? = database.apps.get(id.id)

    // region Internal stuff

    // private var processedLicenses = mutableListOf<CMsgClientLicenseList_License>()
    internal var appIds: List<Int> = emptyList()
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
        KSteamLogging.logVerbose("Pics:HandleLicenses", "Got licenses: ${licenses.size}")

        database.packages.initialize()
        database.apps.initialize()

        val requiresUpdate = licenses.filter { sLicense ->
            if (sLicense.package_id != null && sLicense.change_number != null) {
                val packageId = sLicense.package_id ?: return@filter false
                val changeNumber = sLicense.change_number ?: return@filter false

                database.packages.containsKey(packageId).not() || changeNumber > database.getChangeNumberFor(PicsVdfKvDatabase.Keys.Packages, packageId)
            } else {
                false
            }
        }

        updatePackagesMetadata(requiresUpdate)
        appIds = checkAppsIntegrity(licenses)

        _isPicsAvailable.value = PicsState.Ready
    }

    private suspend fun updatePackagesMetadata(requiresUpdate: List<CMsgClientLicenseList_License>) {
        _isPicsAvailable.value = PicsState.UpdatingPackages

        if (requiresUpdate.isEmpty()) {
            KSteamLogging.logDebug("Pics:HandleLicenses", "Package metadata is in up-to-date state, no update required")
            return
        } else {
            KSteamLogging.logDebug("Pics:HandleLicenses", "Requesting metadata for ${requiresUpdate.size} packages")
        }

        dispatchListProcessing(loadPackageInfo(requiresUpdate).distinctBy { it.packageid }) { packageInfoProto ->
            val packageId = packageInfoProto.packageid ?: return@dispatchListProcessing null
            val changeNumber = packageInfoProto.change_number?.toUInt()?.toLong() ?: return@dispatchListProcessing null
            val buffer = packageInfoProto.buffer?.toByteArray() ?: return@dispatchListProcessing null

            database.parseBinaryVdf<PackageInfo>(buffer)?.also { packageInfo ->
                database.putPicsMetadata(PicsVdfKvDatabase.Keys.Packages, packageId, changeNumber, buffer)
                database.packages.put(packageId, packageInfo)
            }?.appIds
        }
    }

    // This is trying to mimic Steam client behavior
    private suspend fun checkAppsIntegrity(licenses: List<CMsgClientLicenseList_License>): List<Int> {
        // Get ids of app that we actually own
        val appIds = licenses
            .mapNotNull { database.packages.get(it.package_id ?: return@mapNotNull null)?.appIds }
            .flatten()
            .distinct()

        // Firstly, we request app tokens to access metadata
        val tokens = steamClient.execute(SteamPacket.newProto(
            messageId = EMsg.k_EMsgClientPICSAccessTokenRequest,
            adapter = CMsgClientPICSAccessTokenRequest.ADAPTER,
            payload = CMsgClientPICSAccessTokenRequest(appids = appIds.toList())
        )).getProtoPayload(CMsgClientPICSAccessTokenResponse.ADAPTER).data.app_access_tokens

        // Firstly, we load cloud app metadata
        val metadata = loadAppsInfo(tokens, withoutContent = true)

        // Secondly, we diff the change numbers
        val requiresUpdate = metadata
            .filter { sAppInfo ->
                val appId = sAppInfo.appid ?: return@filter false
                val changeNumber = sAppInfo.change_number ?: return@filter false

                database.apps.containsKey(appId).not() || changeNumber > database.getChangeNumberFor(PicsVdfKvDatabase.Keys.Apps, appId)
            }

        if (requiresUpdate.isEmpty()) {
            KSteamLogging.logDebug("Pics:HandleLicenses", "App metadata is in up-to-date state, no update required")
            return appIds
        } else {
            KSteamLogging.logDebug("Pics:HandleLicenses", "Requesting metadata for ${requiresUpdate.size} apps")
        }

        _isPicsAvailable.value = PicsState.UpdatingApps

        dispatchListProcessing(loadAppsInfo(tokens, withoutContent = false).distinctBy { it.appid }) { appInfoProto ->
            val appId = appInfoProto.appid ?: return@dispatchListProcessing null
            val changeNumber = appInfoProto.change_number?.toUInt()?.toLong() ?: return@dispatchListProcessing null
            val buffer = appInfoProto.buffer?.toByteArray() ?: return@dispatchListProcessing null

            database.parseTextVdf<AppInfo>(buffer)?.also { appInfo ->
                database.putPicsMetadata(PicsVdfKvDatabase.Keys.Apps, appId, changeNumber, buffer)
                database.apps.put(appId, appInfo)
            }
        }

        return appIds
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

    private suspend fun loadAppsInfo(tokens: List<CMsgClientPICSAccessTokenResponse_AppToken>, withoutContent: Boolean = false): List<CMsgClientPICSProductInfoResponse_AppInfo> {
        return requestDataFromPics(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = withoutContent,
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

    override suspend fun getMetadataFor(appIds: List<AppId>) = getAppSummariesByAppId(appIds)
}