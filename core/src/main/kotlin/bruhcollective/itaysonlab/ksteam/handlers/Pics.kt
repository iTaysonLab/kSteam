package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamDatabase
import bruhcollective.itaysonlab.ksteam.database.entities.PicsApp
import bruhcollective.itaysonlab.ksteam.database.entities.PicsPackage
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.library.DynamicFilters
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import bruhcollective.itaysonlab.kxvdf.RootNodeSkipperDeserializationStrategy
import bruhcollective.itaysonlab.kxvdf.Vdf
import bruhcollective.itaysonlab.kxvdf.decodeFromBufferedSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import okio.*
import steam.webui.common.*

/**
 * A handler to access the PICS infrastructure
 */
class Pics internal constructor(
    private val steamClient: SteamClient,
    private val database: KSteamDatabase
) : BaseHandler {
    @OptIn(ExperimentalSerializationApi::class)
    private val vdfAppInfo = Vdf {
        ignoreUnknownKeys = true
    }

    private var processedLicenses = mutableListOf<CMsgClientLicenseList_License>()

    private val _isPicsAvailable = MutableStateFlow(PicsState.Initialization)
    val isPicsAvailable = _isPicsAvailable.asStateFlow()

    // TODO: filter based on owning package ids
    internal var appIds = emptyList<AppId>()
        private set

    @OptIn(ExperimentalSerializationApi::class)
    private val vdfBinary = Vdf {
        ignoreUnknownKeys = true
        binaryFormat = true
        readFirstInt = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val vdfText = Vdf {
        ignoreUnknownKeys = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getAppIdsAsInfos(ids: List<AppId>): List<AppInfo> = database.withDatabase {
        PicsApp.getVdfByAppId(this, ids)
    }.map { blob ->
        blob.inputStream.source().buffer().use { source ->
            vdfAppInfo.decodeFromBufferedSource(RootNodeSkipperDeserializationStrategy(), source)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getAppIdsFiltered(filters: DynamicFilters): List<AppInfo> = database.withDatabase {
        PicsApp.getVdfByFilter(this, filters)
    }.map { blob ->
        blob.inputStream.source().buffer().use { source ->
            vdfAppInfo.decodeFromBufferedSource(RootNodeSkipperDeserializationStrategy(), source)
        }
    }

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

        val allDatabasePackages = database.withDatabase {
            PicsPackage.selectAllAsMap(this)
        }

        // TODO: get latest update number and compare changes

        val requiresUpdate = licenses.filter { sLicense ->
            allDatabasePackages[sLicense.package_id!!.toUInt()].let { dLicense ->
                dLicense == null || sLicense.change_number!!.toUInt() > dLicense
            }
        }

        logVerbose("Pics:HandleLicenses", "Require update: ${requiresUpdate.size}")

        if (requiresUpdate.isNotEmpty()) {
            _isPicsAvailable.value = PicsState.Updating
            requestPicsMetadataForLicenses(requiresUpdate)
        }

        // TODO: check integrity and update if not all ids are present

        appIds = database.withDatabase {
            PicsApp.getAppIds(this)
        }

        _isPicsAvailable.value = PicsState.Ready
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun requestPicsMetadataForLicenses(requiresUpdate: List<CMsgClientLicenseList_License>) {
        val appIds = dispatchListParsing(loadPackageInfo(requiresUpdate)) { pkgInfo ->
            vdfBinary.decodeFromBufferedSource<PackageInfo>(RootNodeSkipperDeserializationStrategy(), Buffer().also { buffer ->
                buffer.write(pkgInfo.buffer ?: return@dispatchListParsing null)
            }).let {
                PicsPackage.PicsPackageVdfRepresentation(Triple(it, (pkgInfo.change_number ?: 0).toUInt(), pkgInfo.buffer ?: ByteString.EMPTY))
            }
        }.also { savePackagesToDatabase(it) }.map { it.packageInfo.appIds }.flatten()

        // We will just assume that any changed packages means that all games are updated
        // However, we should have been used saved access tokens

        dispatchListParsing(loadAppsInfo(appIds)) { appInfo ->
            try {
                vdfText.decodeFromBufferedSource<AppInfo>(
                    RootNodeSkipperDeserializationStrategy(),
                    Buffer().also { buffer ->
                        buffer.write(appInfo.buffer ?: return@dispatchListParsing null)
                    }).let {
                        PicsApp.PicsAppVdfRepresentation(Triple(it, (appInfo.change_number ?: 0).toUInt(), appInfo.buffer ?: ByteString.EMPTY))
                    }
            } catch (mfe: Exception) {
                // Some of appids might NOT be the games
                // In kSteam scope, this is not required (as we are not building a full Steam replacement client and probably no one will do it)
                logVerbose("Pics:Unknown", appInfo.buffer?.hex().orEmpty() )
                null
            }
        }.also { saveAppsToDatabase(it) }
    }

    private suspend fun savePackagesToDatabase(info: List<PicsPackage.PicsPackageVdfRepresentation>) {
        database.withDatabase {
            PicsPackage.insertAll(this, info)
        }
    }

    private suspend fun saveAppsToDatabase(info: List<PicsApp.PicsAppVdfRepresentation>) {
        database.withDatabase {
            PicsApp.insertAll(this, info)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <In, Out> dispatchListParsing(data: List<In>, transformer: suspend (In) -> Out?): List<Out> = coroutineScope {
        val parseDispatcher = Dispatchers.IO.limitedParallelism(8)

        data.map {
            async(parseDispatcher) {
                transformer(it)
            }
        }.awaitAll().filterNotNull()
    }

    enum class PicsState {
        // 1. Requesting information from the server
        Initialization,
        // 2. Updating PICS information
        Updating,
        // 3. PICS is ready to use
        Ready
    }
}