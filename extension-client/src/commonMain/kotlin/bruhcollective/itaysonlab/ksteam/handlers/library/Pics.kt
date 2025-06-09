package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRoomDatabase
import bruhcollective.itaysonlab.ksteam.database.room.dao.RoomPicsApplicationDao.PendingApplicationEntry
import bruhcollective.itaysonlab.ksteam.database.room.dao.RoomPicsPackageDao.PendingPackageEntry
import bruhcollective.itaysonlab.ksteam.database.room.entity.apps.RoomPackageLicense
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsAppEntry
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsPackageEntry
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplicationFactory
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplicationLicense
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import bruhcollective.itaysonlab.kxvdf.RootNodeSkipperDeserializationStrategy
import bruhcollective.itaysonlab.kxvdf.Vdf
import bruhcollective.itaysonlab.kxvdf.decodeFromBufferedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Buffer
import okio.ByteString.Companion.toByteString
import steam.webui.common.*
import kotlin.math.max

/**
 * A handler to access the PICS infrastructure
 */
@OptIn(ExperimentalSerializationApi::class)
class Pics internal constructor(
    private val steamClient: ExtendedSteamClient,
    internal val database: KSteamRoomDatabase
) {
    private companion object {
        private const val TAG = "PicsHandler"
        private const val PERSISTENCE_KEY = "pics_last_change_number"
    }

    //

    private val _isPicsAvailable = MutableStateFlow(false)
    val isPicsAvailable = _isPicsAvailable.asStateFlow()

    private val _picsInitializationProgress = MutableStateFlow(0f)
    val picsInitializationProgress = _picsInitializationProgress.asStateFlow()

    /**
     * Suspends execution until PICS subsystem is ready to be used (all packages/apps verified and inserted into DB).
     */
    suspend fun awaitPicsInitialization() {
        _isPicsAvailable.first { it }
    }

    /**
     * Queries an application in PICS subsystem, returning [SteamApplication] if an app is found.
     *
     * @return [SteamApplication], or null if app is not found in the database or PICS infrastructure was not ready yet
     */
    suspend fun getSteamApplication(full: Boolean, id: Int): SteamApplication? {
        return if (full) {
            SteamApplicationFactory.fromDatabase(database.sharedDatabase.picsApplications().getFullApplicationById(id) ?: return null)
        } else {
            SteamApplicationFactory.fromDatabase(database.sharedDatabase.picsApplications().getApplicationById(id) ?: return null)
        }
    }

    /**
     * Queries multiple applications in PICS subsystem, returning [SteamApplication]s if any apps are found.
     *
     * @return a list of [SteamApplication]. Some or all elements might be missing due to absence of apps in DB or PICS infrastructure was not ready yet
     */
    suspend fun getSteamApplications(full: Boolean, vararg ids: Int): List<SteamApplication> {
        return getSteamApplications(full, ids.toList())
    }

    /**
     * Queries multiple applications in PICS subsystem, returning [SteamApplication]s if any apps are found.
     *
     * @return a list of [SteamApplication]. Some or all elements might be missing due to absence of apps in DB or PICS infrastructure was not ready yet
     */
    suspend fun getSteamApplications(full: Boolean, ids: List<Int>): List<SteamApplication> {
        return if (full) {
            database.sharedDatabase.picsApplications().getFullApplicationByIds(ids).map(SteamApplicationFactory::fromDatabase)
        } else {
            database.sharedDatabase.picsApplications().getApplicationByIds(ids).map(SteamApplicationFactory::fromDatabase)
        }
    }

    /**
     * Finds application licenses for the current account.
     */
    suspend fun findLicensesForCurrentUser(appId: AppId): List<SteamApplicationLicense> {
        return database.sharedDatabase.picsPackages().getGrantedAppConnection(appId.value)?.let { packageId ->
            database.currentUserDatabase.packageLicenses().getLicensesByPackageId(packageId)
        }.orEmpty().map(RoomPackageLicense::convert)
    }

    // region Internal stuff

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
            runCatching {
                handleServerLicenseList(CMsgClientLicenseList.ADAPTER.decode(packet.payload).licenses)
            }.onFailure { e ->
                steamClient.logger.logError(TAG) { "[handleLicenses] error = ${e.message}" }
                e.printStackTrace()
            }.onSuccess {
                steamClient.logger.logDebug(TAG) { "[handleLicenses] ready!" }
            }
        }
    }

    private suspend fun handleServerLicenseList(licenses: List<CMsgClientLicenseList_License>) {
        steamClient.logger.logDebug(TAG) { "[handleServerLicenseList] received ${licenses.size} licenses" }

        val changesResponse = steamClient.awaitProto(
            packet = SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientPICSChangesSinceRequest,
                payload = CMsgClientPICSChangesSinceRequest(
                    since_change_number = steamClient.persistence.getInt(PERSISTENCE_KEY),
                    send_app_info_changes = true,
                    send_package_info_changes = true
                )
            ), adapter = CMsgClientPICSChangesSinceResponse.ADAPTER
        )

        steamClient.logger.logDebug(TAG) { "[handleServerLicenseList] changes: since = ${changesResponse.since_change_number} -> ${changesResponse.current_change_number}, full = ${changesResponse.force_full_update}, apps = ${changesResponse.app_changes.size} [${changesResponse.force_full_app_update}], packages = ${changesResponse.package_changes.size} [${changesResponse.force_full_package_update}]" }

        _picsInitializationProgress.value = 0f
        if (changesResponse.force_full_update == true) {
            steamClient.logger.logDebug(TAG) { "-> API requested forced full update" }

            // Short-circuit to full database update
            requestAndWritePackageChunks(tokens = licenses.associate { it.package_id!! to it.access_token }, 0.5f)

            _picsInitializationProgress.value = 0.5f
            requestTokensAndUpdate(appIds = licenses.mapNotNull { it.package_id }.let { database.sharedDatabase.picsPackages().getGrantedAppsForPackages(it) }, 0.5f)
        } else {
            steamClient.logger.logDebug(TAG) { "-> doing partial update" }

            // Update known packages by using PICS response
            if (changesResponse.force_full_package_update == true) {
                steamClient.logger.logDebug(TAG) { "-> full pkg update" }
                requestAndWritePackageChunks(tokens = licenses.associate { it.package_id!! to it.access_token }, 0.5f)
            } else if (changesResponse.package_changes.isNotEmpty()) {
                steamClient.logger.logDebug(TAG) { "-> partial pkg update" }
                changesResponse.package_changes.associate { appChange ->
                    (appChange.packageid!! to database.sharedDatabase.picsEntries().getAccessTokenForPackage(appChange.packageid!!))
                }.filterValues { it != null }.let { packages -> requestAndWritePackageChunks(packages, progressFactor = 0.25f) }
            }

            _picsInitializationProgress.value = 0.25f

            // Update known apps by using PICS response
            if (changesResponse.force_full_app_update == true) {
                steamClient.logger.logDebug(TAG) { "-> full app update" }
                requestTokensAndUpdate(appIds = licenses.mapNotNull { it.package_id }.let { database.sharedDatabase.picsPackages().getGrantedAppsForPackages(it) }, 0.25f)
            } else if (changesResponse.app_changes.isNotEmpty()) {
                steamClient.logger.logDebug(TAG) { "-> partial app update" }
                changesResponse.app_changes.associate { appChange ->
                    appChange.appid!! to database.sharedDatabase.picsEntries().getAccessTokenForApp(appChange.appid!!)
                }.filterValues { it != null }.let { apps -> requestAndWriteAppChunks(apps, progressFactor = 0.25f) }
            }

            // If license size differs, also scan for unknown packages
            if (licenses.size != database.currentUserDatabase.packageLicenses().count()) {
                _picsInitializationProgress.value = 0.5f
                checkForMissingEntries(licenses)
            }
        }

        // Save persistence
        steamClient.persistence.set(PERSISTENCE_KEY, changesResponse.current_change_number ?: max(database.sharedDatabase.picsEntries().getPackageLastChangeNumber(), database.sharedDatabase.picsEntries().getAppLastChangeNumber()).toInt())

        // Write licenses
        writeAccountSpecificLicenseInformation(licenses)
        _picsInitializationProgress.value = 1f
        _isPicsAvailable.value = true
        steamClient.logger.logDebug(TAG) { "[handleServerLicenseList] OK" }
    }

    private suspend fun checkForMissingEntries(licenses: List<CMsgClientLicenseList_License>) {
        steamClient.logger.logDebug(TAG) { "[checkForMissingEntries]" }

        val newPackages = licenses.filter { sLicense ->
            database.currentUserDatabase.packageLicenses().getLicensesByPackageId(sLicense.package_id!!).isEmpty()
        }

        // Write new packages
        requestAndWritePackageChunks(newPackages.associate { pkg -> pkg.package_id!! to pkg.access_token }, progressFactor = 0.25f)

        // Get the apps from them
        requestTokensAndUpdate(database.sharedDatabase.picsPackages().getGrantedAppsForPackages(newPackages.map { it.package_id!! }), progressFactor = 0.25f)
    }

    private suspend fun requestTokensAndUpdate(appIds: List<Int>, progressFactor: Float) {
        steamClient.awaitProto(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientPICSAccessTokenRequest,
                payload = CMsgClientPICSAccessTokenRequest(appids = appIds.toList())
            ), adapter = CMsgClientPICSAccessTokenResponse.ADAPTER
        ).app_access_tokens.filter { token ->
            token.appid != null
        }.associate {
            it.appid!! to it.access_token
        }.let { tokens ->
            requestAndWriteAppChunks(tokens, progressFactor)
        }
    }

    private suspend fun requestAndWritePackageChunks(tokens: Map<Int, Long?>, progressFactor: Float) {
        if (tokens.isEmpty()) return

        awaitStreamedPicsRequest(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                packages = tokens.entries.map { (id, token) ->
                    CMsgClientPICSProductInfoRequest_PackageInfo(
                        packageid = id,
                        access_token = token,
                    )
                }
            ), selector = CMsgClientPICSProductInfoResponse::packages
        ) { chunk ->
            writePicsPackageChunk(tokens, chunk)
            _picsInitializationProgress.update { value -> value + (progressFactor * (chunk.size.toFloat() / tokens.size.toFloat())) }
        }
    }

    private suspend fun requestAndWriteAppChunks(tokens: Map<Int, Long?>, progressFactor: Float) {
        if (tokens.isEmpty()) return

        awaitStreamedPicsRequest(
            request = CMsgClientPICSProductInfoRequest(
                meta_data_only = false,
                apps = tokens.map { (appId, accessToken) ->
                    CMsgClientPICSProductInfoRequest_AppInfo(appid = appId, access_token = accessToken)
                }
            ), selector = CMsgClientPICSProductInfoResponse::apps
        ) { chunk ->
            writePicsAppChunk(tokens, chunk)
            _picsInitializationProgress.update { value -> value + (progressFactor * (chunk.size.toFloat() / tokens.size.toFloat())) }
        }
    }

    private suspend fun writePicsPackageChunk(
        licenseMap: Map<Int, Long?>,
        chunk: List<CMsgClientPICSProductInfoResponse_PackageInfo>
    ) {
        steamClient.logger.logVerbose(TAG) { "[writePicsPackageChunk] size = ${chunk.size}" }

        chunk.fold(PendingPackageEntry()) { entry, packageInfo ->
            val changeNumber = packageInfo.change_number ?: return@fold entry
            val buffer = packageInfo.buffer?.toByteArray() ?: return@fold entry
            val parsedPackageInfo = parseBinaryVdf<PackageInfo>(buffer) ?: return@fold entry

            entry += RoomPicsPackageEntry(id = parsedPackageInfo.packageId, changeNumber = changeNumber, accessToken = licenseMap[parsedPackageInfo.packageId] ?: 0L)
            entry += parsedPackageInfo

            entry
        }.let { pendingEntry ->
            database.sharedDatabase.picsPackages().upsertPicsPackage(pendingEntry)
        }
    }

    private suspend fun writePicsAppChunk(
        licenseMap: Map<Int, Long?>,
        chunk: List<CMsgClientPICSProductInfoResponse_AppInfo>
    ) {
        steamClient.logger.logVerbose(TAG) { "[writePicsAppChunk] size = ${chunk.size}" }

        chunk.fold(PendingApplicationEntry()) { entry, appInfo ->
            val changeNumber = appInfo.change_number ?: return@fold entry
            val buffer = appInfo.buffer?.toByteArray() ?: return@fold entry
            val parsedAppInfo = parseTextVdf<AppInfo>(buffer) ?: return@fold entry

            entry += RoomPicsAppEntry(id = parsedAppInfo.appId, changeNumber = changeNumber, accessToken = licenseMap[parsedAppInfo.appId] ?: 0L)
            entry += parsedAppInfo

            entry
        }.let { pendingEntry ->
            database.sharedDatabase.picsApplications().upsertAppInfo(pendingEntry)
        }
    }

    private suspend fun <T> awaitStreamedPicsRequest(
        request: CMsgClientPICSProductInfoRequest,
        selector: (CMsgClientPICSProductInfoResponse) -> List<T>,
        process: suspend (List<T>) -> Unit
    ) {
        steamClient.awaitStreamedMultipleProto(
            packet = SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientPICSProductInfoRequest,
                payload = request
            ), adapter = CMsgClientPICSProductInfoResponse.ADAPTER, process = { response ->
                process(selector(response))
                response.response_pending == null || response.response_pending == false
            }
        )
    }

    // endregion

    // region Account-specific license information

    private suspend fun writeAccountSpecificLicenseInformation(licenses: List<CMsgClientLicenseList_License>) {
        database.currentUserDatabase.packageLicenses().deleteAll()
        database.currentUserDatabase.packageLicenses().insert(licenses.map(::RoomPackageLicense))
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