package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.library.DynamicFilters
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PicsAppChangeNumber
import bruhcollective.itaysonlab.ksteam.models.pics.PicsPackageChangeNumber
import bruhcollective.itaysonlab.kxvdf.RootNodeSkipperDeserializationStrategy
import bruhcollective.itaysonlab.kxvdf.Vdf
import bruhcollective.itaysonlab.kxvdf.decodeFromBufferedSource
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Buffer
import okio.ByteString.Companion.toByteString
import steam.webui.common.*

/**
 * A handler to access the PICS infrastructure
 */
@OptIn(ExperimentalSerializationApi::class)
class Pics internal constructor(
    private val steamClient: ExtendedSteamClient,
    internal val database: KSteamRealmDatabase
) {
    private val _isPicsAvailable = MutableStateFlow(PicsState.Initialization)
    val isPicsAvailable = _isPicsAvailable.asStateFlow()

    /**
     * Suspends execution until PICS subsystem is ready to be used (all packages/apps verified and inserted into DB).
     */
    suspend fun awaitPicsInitialization() {
        _isPicsAvailable.first { it == PicsState.Ready }
    }

    /**
     * Queries an application in PICS subsystem, returning [SteamApplication] if an app is found.
     *
     * @return [SteamApplication], or null if app is not found in the database or PICS infrastructure was not ready yet
     */
    fun getSteamApplication(id: Int): SteamApplication? {
        return SteamApplication.fromPics(
            database.sharedRealm.query<AppInfo>("appId == $0", id).first().find() ?: return null
        )
    }

    /**
     * Queries multiple applications in PICS subsystem, returning [SteamApplication]s if any apps are found.
     *
     * @return a list of [SteamApplication]. Some or all elements might be missing due to absence of apps in DB or PICS infrastructure was not ready yet
     */
    fun getSteamApplications(ids: List<Int>): List<SteamApplication> {
        return database.sharedRealm.query<AppInfo>("appId IN $0", ids).find().map(SteamApplication::fromPics)
    }

    /**
     * Queries multiple applications in PICS subsystem, returning [SteamApplication]s if any apps are found.
     *
     * @return a list of [SteamApplication]. Some or all elements might be missing due to absence of apps in DB or PICS infrastructure was not ready yet
     */
    fun getSteamApplications(vararg ids: Int): List<SteamApplication> = getSteamApplications(ids.toList())

    /**
     * Queries an application list in PICS subsystem, returning [SteamApplication]s filtered by filters.
     *
     * @return [SteamApplication], or null if app is not found in the database or PICS infrastructure was not ready yet
     */
    fun querySteamApplicationsByFilter(filters: DynamicFilters): List<SteamApplication> {
        return composeRealmQueryByFilters(filters).find().map(SteamApplication::fromPics)
    }

    /**
     * Queries an application list in PICS subsystem, returning [SteamApplication]s filtered by filters. Returns a [Flow] that dynamically updates based on library changes.
     *
     * @return [SteamApplication], or null if app is not found in the database or PICS infrastructure was not ready yet
     */
    fun flowSteamApplicationsByFilter(filters: DynamicFilters): Flow<List<SteamApplication>> {
        return composeRealmQueryByFilters(filters).asFlow().map {
            it.list.map(SteamApplication::fromPics)
        }
    }

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
    init {
        steamClient.on(EMsg.k_EMsgClientLicenseList) { packet ->
            handleServerLicenseList(CMsgClientLicenseList.ADAPTER.decode(packet.payload).licenses)
        }
    }

    private suspend fun handleServerLicenseList(licenses: List<CMsgClientLicenseList_License>) {
        steamClient.logger.logDebug("Pics:HandleLicenses") { "Got licenses: ${licenses.size}" }

        val requiresUpdate = withContext(Dispatchers.Default) {
            licenses.filter { sLicense ->
                if (sLicense.package_id != null && sLicense.change_number != null) {
                    val packageId = sLicense.package_id ?: return@filter false
                    val changeNumber = sLicense.change_number ?: return@filter false
                    val databaseChangeNumber = database.sharedRealm.query<PicsPackageChangeNumber>("packageId == $0", packageId).first().find()?.changeNumber

                    // if null, package was NOT cached
                    databaseChangeNumber == null || changeNumber > databaseChangeNumber
                } else {
                    false
                }
            }
        }

        withContext(Dispatchers.IO) {
            updatePackagesMetadata(requiresUpdate)
            checkAppsIntegrity(licenses)
        }

        steamClient.logger.logDebug("Pics:HandleLicenses") { "PICS subsystem initialized and is ready to use!" }

        _isPicsAvailable.value = PicsState.Ready
    }

    private suspend fun updatePackagesMetadata(requiresUpdate: List<CMsgClientLicenseList_License>) {
        _isPicsAvailable.value = PicsState.UpdatingPackages

        if (requiresUpdate.isEmpty()) {
            steamClient.logger.logDebug("Pics:UpdatePackagesMetadata") {
                "Package metadata is in up-to-date state, no update required"
            }
            return
        } else {
            steamClient.logger.logDebug("Pics:UpdatePackagesMetadata") {
                "Requesting metadata for ${requiresUpdate.size} packages"
            }
        }

        picsFlow(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                packages = requiresUpdate.map {
                    CMsgClientPICSProductInfoRequest_PackageInfo(
                        packageid = it.package_id,
                        access_token = it.access_token,
                    )
                }
            ), selector = CMsgClientPICSProductInfoResponse::packages
        ).collect(::writePicsPackageChunk)
    }

    // This is trying to mimic Steam client behavior
    private suspend fun checkAppsIntegrity(licenses: List<CMsgClientLicenseList_License>) {
        // Get ids of app that we actually own
        appIds = licenses.mapNotNull { it.package_id }
            .let { owningPackages -> database.sharedRealm.query<PackageInfo>("packageId IN $0", owningPackages).find() }
            .flatMap { it.appIds }
            .distinct()

        // We request app tokens to access any metadata at all
        val tokens = steamClient.execute(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientPICSAccessTokenRequest,
                adapter = CMsgClientPICSAccessTokenRequest.ADAPTER,
                payload = CMsgClientPICSAccessTokenRequest(appids = appIds.toList())
            )
        ).payload.let(CMsgClientPICSAccessTokenResponse.ADAPTER::decode).app_access_tokens.filter { token ->
            token.appid != null
        }.associate {
            it.appid!! to it.access_token
        }

        if (database.sharedRealm.query<AppInfo>().count().find() > 0) {
            // Not a new launch, we should check everything
            partiallyRefreshAppIds(tokens)
        } else {
            // Short-circuit through the non-content PICS requests and immediately request everything
            fullyRefreshAppInfos(tokens)
        }
    }

    private suspend fun fullyRefreshAppInfos(tokens: Map<Int, Long?>) {
        steamClient.logger.logDebug("Pics:HandleLicenses") { "[Full] Requesting metadata for ${tokens.size} apps" }
        _isPicsAvailable.value = PicsState.UpdatingApps

        // And now, we request those apps and write them!

        picsFlow(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                apps = tokens.map { (appId, accessToken) ->
                    CMsgClientPICSProductInfoRequest_AppInfo(appid = appId, access_token = accessToken)
                }
            ), selector = CMsgClientPICSProductInfoResponse::apps
        ).collect(::writePicsAppChunk)
    }

    private suspend fun partiallyRefreshAppIds(tokens: Map<Int, Long?>) {
        // Here, we add app IDs that we will update from PICS
        val appIdsToUpdate = mutableListOf<Int>()

        // Then, we load cloud app metadata
        picsFlow(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = true,
                apps = tokens.map { (appId, accessToken) ->
                    CMsgClientPICSProductInfoRequest_AppInfo(appid = appId, access_token = accessToken)
                }
            ), selector = CMsgClientPICSProductInfoResponse::apps
        ).collect { metadataChunk ->
            // Here, we diff the change numbers and note what apps we are missing in the DB
            for (appInfo in metadataChunk) {
                val appId = appInfo.appid ?: continue
                val changeNumber = appInfo.change_number ?: continue
                val databaseChangeNumber = database.sharedRealm.query<PicsAppChangeNumber>("appId == $0", appId).first().find()?.changeNumber

                if (databaseChangeNumber == null) {
                    steamClient.logger.logVerbose("Pics:HandleLicenses") {
                        "[$appId] not yet cached: $changeNumber"
                    }

                    appIdsToUpdate.add(appId)
                } else if (changeNumber > databaseChangeNumber) {
                    steamClient.logger.logVerbose("Pics:HandleLicenses") {
                        "[$appId] is outdated: $changeNumber > $databaseChangeNumber"
                    }

                    appIdsToUpdate.add(appId)
                } // otherwise it is actual
            }
        }

        if (appIdsToUpdate.isEmpty()) {
            steamClient.logger.logDebug("Pics:HandleLicenses") { "App metadata is in up-to-date state, no update required" }
            return
        }

        steamClient.logger.logDebug("Pics:HandleLicenses") { "[Partial] Requesting metadata for ${appIdsToUpdate.size} apps" }
        _isPicsAvailable.value = PicsState.UpdatingApps

        // And now, we request those apps and write them!

        picsFlow(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                apps = appIdsToUpdate.map { appId ->
                    CMsgClientPICSProductInfoRequest_AppInfo(appid = appId, access_token = tokens[appId])
                }
            ), selector = CMsgClientPICSProductInfoResponse::apps
        ).collect(::writePicsAppChunk)
    }

    private suspend fun <T> picsFlow(
        request: CMsgClientPICSProductInfoRequest,
        selector: (CMsgClientPICSProductInfoResponse) -> List<T>
    ): Flow<List<T>> {
        return steamClient.subscribe(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientPICSProductInfoRequest,
                adapter = CMsgClientPICSProductInfoRequest.ADAPTER,
                payload = request
            )
        ).transformWhile { packet ->
            CMsgClientPICSProductInfoResponse.ADAPTER.decode(packet.payload).let { response ->
                emit(selector(response))
                response.response_pending ?: false
            }
        }
    }

    private suspend fun writePicsPackageChunk(
        chunk: List<CMsgClientPICSProductInfoResponse_PackageInfo>
    ) {
        steamClient.logger.logDebug("Pics:HandleLicenses") { "Processing PICS batch: ${chunk.size} packages received!" }

        database.sharedRealm.write {
            for (packageInfo in chunk) {
                val changeNumber = packageInfo.change_number ?: continue
                val buffer = packageInfo.buffer?.toByteArray() ?: continue
                val parsedPackageInfo = parseBinaryVdf<PackageInfo>(buffer) ?: continue

                copyToRealm(PicsPackageChangeNumber().apply {
                    this.packageId = parsedPackageInfo.packageId
                    this.changeNumber = changeNumber
                }, updatePolicy = UpdatePolicy.ALL)

                copyToRealm(parsedPackageInfo, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    private suspend fun writePicsAppChunk(
        chunk: List<CMsgClientPICSProductInfoResponse_AppInfo>
    ) {
        steamClient.logger.logDebug("Pics:HandleLicenses") { "Processing PICS batch: ${chunk.size} apps received!" }

        database.sharedRealm.write {
            for (appInfo in chunk) {
                val changeNumber = appInfo.change_number ?: continue
                val buffer = appInfo.buffer?.toByteArray() ?: continue
                val parsedAppInfo = parseTextVdf<AppInfo>(buffer) ?: continue

                copyToRealm(PicsAppChangeNumber().apply {
                    this.appId = parsedAppInfo.appId
                    this.changeNumber = changeNumber
                }, updatePolicy = UpdatePolicy.ALL)

                copyToRealm(parsedAppInfo, updatePolicy = UpdatePolicy.ALL)
            }
        }
    }

    // endregion

    // region VDF parsing

    private val vdfBinary = Vdf {
        ignoreUnknownKeys = true
        binaryFormat = true
        readFirstInt = true
    }

    private val vdfText = Vdf {
        ignoreUnknownKeys = true
        binaryFormat = false
    }

    private inline fun <reified T> parseBinaryVdf(source: ByteArray) = parseVdf<T>(vdfBinary, source)
    private inline fun <reified T> parseTextVdf(source: ByteArray) = parseVdf<T>(vdfText, source)

    private inline fun <reified T> parseVdf(vdf: Vdf, source: ByteArray): T? {
        return try {
            vdf.decodeFromBufferedSource<T>(RootNodeSkipperDeserializationStrategy(), Buffer().also { buffer ->
                buffer.write(source)
            })
        } catch (mfe: Exception) {
            // We try to cover almost all types, but sometimes stuff... happens
            steamClient.logger.logVerbose("Pics:Unknown") { source.toByteString().hex() }
            mfe.printStackTrace()
            null
        }
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