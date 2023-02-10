package bruhcollective.itaysonlab.ksteam.handlers

import app.cash.sqldelight.async.coroutines.awaitAsList
import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.pics.asPicsDb
import bruhcollective.itaysonlab.ksteam.persist.PicsApp
import bruhcollective.itaysonlab.ksteam.pics.PicsDatabase
import bruhcollective.itaysonlab.ksteam.pics.model.AppInfo
import bruhcollective.itaysonlab.ksteam.pics.model.PackageInfo
import bruhcollective.itaysonlab.kxvdf.RootNodeSkipperDeserializationStrategy
import bruhcollective.itaysonlab.kxvdf.Vdf
import bruhcollective.itaysonlab.kxvdf.decodeFromBufferedSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Buffer
import okio.ByteString
import steam.webui.common.*

/**
 * A handler to access the PICS infrastructure
 */
class Pics(
    private val steamClient: SteamClient
) : BaseHandler {
    private var processedLicenses = mutableListOf<CMsgClientLicenseList_License>()
    private val database = PicsDatabase(steamClient)

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

    suspend fun getPicsAppIds(ids: List<AppId>): List<PicsApp> = database.runOnDatabase {
        picsAppQueries.selectById(ids.map(AppId::asLong)).awaitAsList()
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

        val allDatabasePackages = database.runOnDatabase {
            picsPackageQueries.selectAll().awaitAsList().associateBy { it.id }
        }

        val requiresUpdate = licenses.filter { sLicense ->
            allDatabasePackages[sLicense.package_id!!.toLong()].let { dLicense ->
                dLicense == null || sLicense.change_number!!.toLong() > dLicense.picsChangeNumber
            }
        }

        logVerbose("Pics:HandleLicenses", "Require update: ${requiresUpdate.size}")

        if (requiresUpdate.isNotEmpty()) {
            requestPicsMetadataForLicenses(requiresUpdate)
        }

        appIds = database.runOnDatabase {
            picsAppQueries.selectIds().awaitAsList().map { AppId(it.toInt()) }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun requestPicsMetadataForLicenses(requiresUpdate: List<CMsgClientLicenseList_License>) {
        val appIds = dispatchListParsing(loadPackageInfo(requiresUpdate)) { pkgInfo ->
            vdfBinary.decodeFromBufferedSource<PackageInfo>(RootNodeSkipperDeserializationStrategy(), Buffer().also { buffer ->
                buffer.write(pkgInfo.buffer ?: return@dispatchListParsing null)
            }).let { Triple(it, pkgInfo.change_number ?: 0, pkgInfo.buffer ?: ByteString.EMPTY) }
        }.also { savePackagesToDatabase(it) }.map { it.first.appIds }.flatten()

        // We will just assume that any changed packages means that all games are updated
        // However, we should have been used saved access tokens

        dispatchListParsing(loadAppsInfo(appIds)) { appInfo ->
            try {
                vdfText.decodeFromBufferedSource<AppInfo>(
                    RootNodeSkipperDeserializationStrategy(),
                    Buffer().also { buffer ->
                        buffer.write(appInfo.buffer ?: return@dispatchListParsing null)
                    }).let { Triple(it, appInfo.change_number ?: 0, appInfo.buffer ?: ByteString.EMPTY) }
            } catch (mfe: Exception) {
                // Some of appids might NOT be the games
                // In kSteam scope, this is not required (as we are not building a full Steam replacement client and probably no one will do it)
                logVerbose("Pics:Unknown", appInfo.buffer?.hex().orEmpty() )
                null
            }
        }.also { saveAppsToDatabase(it) }
    }

    private suspend fun savePackagesToDatabase(info: List<Triple<PackageInfo, Int, ByteString>>) {
        database.runOnDatabase {
            picsPackageQueries.transaction {
                info.forEach { packageInfo ->
                    picsPackageQueries.insert(
                        id = packageInfo.first.packageId.toLong(),
                        picsChangeNumber = packageInfo.second.toLong(),
                        picsRawData = packageInfo.third.toByteArray()
                    )
                }
            }
        }
    }

    private suspend fun saveAppsToDatabase(info: List<Triple<AppInfo, Int, ByteString>>) {
        database.runOnDatabase {
            picsAppQueries.transaction {
                info.forEach { appInfo ->
                    picsAppQueries.insert(
                        appInfo.first.asPicsDb(
                            changeNumber = appInfo.second.toLong(),
                            rawData = appInfo.third.toByteArray()
                        )
                    )
                }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <In, Out> dispatchListParsing(data: List<In>, transformer: suspend (In) -> Out?): List<Out> = coroutineScope {
        val parseDispatcher = Dispatchers.IO.limitedParallelism(8)

        data.map {
            async(parseDispatcher) {
                transformer(it)
            }
        }.awaitAll().filterNotNull()
    }
}