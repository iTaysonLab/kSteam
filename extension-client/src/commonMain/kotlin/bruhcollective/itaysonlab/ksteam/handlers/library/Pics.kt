package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.extension.plugins.MetadataPlugin
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
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
    private val steamClient: SteamClient,
    internal val database: KSteamRealmDatabase
) : BaseHandler, MetadataPlugin {
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
            database.realm.query<AppInfo>("appId == $0", id).first().find() ?: return null
        )
    }

    /**
     * Queries multiple applications in PICS subsystem, returning [SteamApplication]s if any apps are found.
     *
     * @return a list of [SteamApplication]. Some or all elements might be missing due to absence of apps in DB or PICS infrastructure was not ready yet
     */
    fun getSteamApplications(ids: List<Int>): List<SteamApplication> {
        return database.realm.query<AppInfo>("appId IN $0", ids).find().map(SteamApplication::fromPics)
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
    override suspend fun onEvent(packet: SteamPacket) {
        if (packet.messageId == EMsg.k_EMsgClientLicenseList) {
            handleServerLicenseList(packet.getProtoPayload(CMsgClientLicenseList.ADAPTER).data.licenses)
        }
    }

    private suspend fun handleServerLicenseList(licenses: List<CMsgClientLicenseList_License>) {
        KSteamLogging.logVerbose("Pics:HandleLicenses") { "Got licenses: ${licenses.size}" }

        val requiresUpdate = withContext(Dispatchers.Default) {
            licenses.filter { sLicense ->
                if (sLicense.package_id != null && sLicense.change_number != null) {
                    val packageId = sLicense.package_id ?: return@filter false
                    val changeNumber = sLicense.change_number ?: return@filter false
                    val databaseChangeNumber = database.realm.query<PicsPackageChangeNumber>("packageId == $0", packageId).first().find()?.changeNumber

                    // if null, package was NOT cached
                    databaseChangeNumber == null || changeNumber > databaseChangeNumber
                } else {
                    false
                }
            }
        }

        updatePackagesMetadata(requiresUpdate)
        appIds = checkAppsIntegrity(licenses)

        _isPicsAvailable.value = PicsState.Ready
    }

    private suspend fun updatePackagesMetadata(requiresUpdate: List<CMsgClientLicenseList_License>) {
        _isPicsAvailable.value = PicsState.UpdatingPackages

        if (requiresUpdate.isEmpty()) {
            KSteamLogging.logDebug("Pics:HandleLicenses") {
                "Package metadata is in up-to-date state, no update required"
            }
            return
        } else {
            KSteamLogging.logDebug("Pics:HandleLicenses") {
                "Requesting metadata for ${requiresUpdate.size} packages"
            }
        }

        loadPackageInfo(requiresUpdate)
            .distinctBy { it.packageid }
            .forEach { packageInfoProto ->
                val packageId = packageInfoProto.packageid ?: return@forEach
                val changeNumber = packageInfoProto.change_number ?: return@forEach
                val buffer = packageInfoProto.buffer?.toByteArray() ?: return@forEach

                parseBinaryVdf<PackageInfo>(buffer)?.also { packageInfo ->
                    database.realm.write {
                        copyToRealm(PicsPackageChangeNumber().apply {
                            this.packageId = packageId
                            this.changeNumber = changeNumber
                        }, updatePolicy = UpdatePolicy.ALL)

                        copyToRealm(packageInfo, updatePolicy = UpdatePolicy.ALL)
                    }
                }
            }
    }

    // This is trying to mimic Steam client behavior
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun checkAppsIntegrity(licenses: List<CMsgClientLicenseList_License>): List<Int> {
        // Get ids of app that we actually own
        val appIds = licenses
            .mapNotNull { it.package_id }
            .let { owningPackages -> database.realm.query<PackageInfo>("packageId IN $0", owningPackages).find() }
            .flatMap { it.appIds }
            .distinct()

        // Firstly, we request app tokens to access metadata
        val tokens = steamClient.execute(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientPICSAccessTokenRequest,
                adapter = CMsgClientPICSAccessTokenRequest.ADAPTER,
                payload = CMsgClientPICSAccessTokenRequest(appids = appIds.toList())
            )
        ).getProtoPayload(CMsgClientPICSAccessTokenResponse.ADAPTER).data.app_access_tokens.associateBy {
            it.appid ?: 0
        }

        // Firstly, we load cloud app metadata
        val metadata = loadAppsInfo(tokens.values, withoutContent = true)

        // Secondly, we diff the change numbers
        val requiresUpdate = metadata.filter { sAppInfo ->
            val appId = sAppInfo.appid ?: return@filter false
            val changeNumber = sAppInfo.change_number ?: return@filter false
            val databaseChangeNumber = database.realm.query<PicsAppChangeNumber>("appId == $0", appId).first().find()?.changeNumber

            // if null, package was NOT cached
            databaseChangeNumber == null || changeNumber > databaseChangeNumber
        }.mapNotNull { tokens[it.appid] }

        if (requiresUpdate.isEmpty()) {
            KSteamLogging.logDebug("Pics:HandleLicenses") {
                "App metadata is in up-to-date state, no update required"
            }
            return appIds
        } else {
            KSteamLogging.logDebug("Pics:HandleLicenses") {
                "Requesting metadata for ${requiresUpdate.size} apps"
            }
        }

        _isPicsAvailable.value = PicsState.UpdatingApps

        loadAppsInfo(requiresUpdate)
            .distinctBy { it.appid }
            .forEach { packageInfoProto ->
                val appId = packageInfoProto.appid ?: return@forEach
                val changeNumber = packageInfoProto.change_number ?: return@forEach
                val buffer = packageInfoProto.buffer?.toByteArray() ?: return@forEach

                parseTextVdf<AppInfo>(buffer)?.also { appInfo ->
                    database.realm.write {
                        copyToRealm(PicsAppChangeNumber().apply {
                            this.appId = appId
                            this.changeNumber = changeNumber
                        }, updatePolicy = UpdatePolicy.ALL)

                        copyToRealm(appInfo, updatePolicy = UpdatePolicy.ALL)
                    }
                }
            }

        return appIds
    }

    private suspend fun loadPackageInfo(licenses: List<CMsgClientLicenseList_License>): List<CMsgClientPICSProductInfoResponse_PackageInfo> {
        if (licenses.isEmpty()) {
            return emptyList()
        }

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

    private suspend fun loadAppsInfo(
        tokens: Collection<CMsgClientPICSAccessTokenResponse_AppToken>,
        withoutContent: Boolean = false
    ): List<CMsgClientPICSProductInfoResponse_AppInfo> {
        if (tokens.isEmpty()) {
            return emptyList()
        }

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

    @Suppress("RemoveExplicitTypeArguments") // this breaks IDE completion
    private suspend fun <T> requestDataFromPics(
        request: CMsgClientPICSProductInfoRequest,
        emitter: (CMsgClientPICSProductInfoResponse) -> List<T>
    ): List<T> {
        val destination = mutableListOf<T>()

        steamClient.subscribe(
            SteamPacket.newProto(
                messageId = EMsg.k_EMsgClientPICSProductInfoRequest,
                adapter = CMsgClientPICSProductInfoRequest.ADAPTER,
                payload = request
            )
        ).transformWhile<SteamPacket, List<T>> { packet ->
            packet.getProtoPayload(CMsgClientPICSProductInfoResponse.ADAPTER).data.let { response ->
                emit(response.let(emitter))
                response.response_pending ?: false
            }
        }.collect { newBatch ->
            destination.addAll(newBatch)
        }

        return destination
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

    internal inline fun <reified T> parseBinaryVdf(source: ByteArray) = parseVdf<T>(vdfBinary, source)
    internal inline fun <reified T> parseTextVdf(source: ByteArray) = parseVdf<T>(vdfText, source)

    internal inline fun <reified T> parseVdf(vdf: Vdf, source: ByteArray): T? {
        return try {
            vdf.decodeFromBufferedSource<T>(
                RootNodeSkipperDeserializationStrategy(),
                Buffer().also { buffer ->
                    buffer.write(source)
                })
        } catch (mfe: Exception) {
            // We try to cover almost all types, but sometimes stuff... happens
            KSteamLogging.logVerbose("Pics:Unknown") { source.toByteString().hex() }
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

    override suspend fun getMetadataFor(appIds: List<Int>): Map<Int, AppSummary> = emptyMap() // getAppSummariesByAppId(appIds)
}