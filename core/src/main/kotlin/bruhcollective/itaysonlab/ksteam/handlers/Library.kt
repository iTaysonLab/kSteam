package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logError
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.models.library.OwnedGame
import bruhcollective.itaysonlab.ksteam.platform.CreateSupervisedCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import steam.webui.common.CMsgClientLogonResponse
import steam.webui.player.CPlayer_GetOwnedGames_Request
import steam.webui.player.CPlayer_GetOwnedGames_Response

/**
 * Provides access to user's owned app library.
 */
class Library(
    private val steamClient: SteamClient
) : BaseHandler {
    private val scope = CreateSupervisedCoroutineScope("LibraryCollector", Dispatchers.Default)
    private var cloudCollector: Job? = null

    private val libraryCache = mutableMapOf<SteamId, List<OwnedGame>>()

    //private val _apps = MutableSharedFlow<>()
    //private val _shelves = MutableSharedFlow<>()
    //private val _collections = MutableSharedFlow<>()

    /**
     * Requests
     */
    suspend fun requestLibrary(steamId: SteamId = steamClient.currentSessionSteamId): List<OwnedGame> {
        libraryCache[steamId]?.let {
            return it
        }

        return steamClient.webApi.execute(
            methodName = "Player.GetOwnedGames",
            requestAdapter = CPlayer_GetOwnedGames_Request.ADAPTER,
            responseAdapter = CPlayer_GetOwnedGames_Response.ADAPTER,
            requestData = CPlayer_GetOwnedGames_Request(
                steamid = steamId.longId,
                include_appinfo = true,
                include_extended_appinfo = true
            )
        ).dataNullable?.games?.map(::OwnedGame).orEmpty().also { libraryCache[steamId] = it }
    }

    suspend fun editCollection() {

    }

    suspend fun createCollection() {

    }

    suspend fun deleteCollection() {

    }

    fun ownsThisApp(appId: AppId) {

    }

    private suspend fun startCollector() {
        // 1. Get owned apps

        scope.launch {
            requestLibrary()
        }

        // 2. Collect WebUI info (if collector is not started)

        if (cloudCollector == null) {
            cloudCollector = steamClient.cloudConfiguration.request(1).onEach { list ->
                // TODO: parse
            }.onCompletion {
                cloudCollector = null

                if (it != null && it !is CancellationException) {
                    logError("Library::Collector", "Error occurred when collecting library data: ${it.message}")
                    it.printStackTrace()
                }
            }.launchIn(scope)
        }
    }

    private suspend fun handleCollection() {

    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientLogOnResponse -> {
                if (packet.getProtoPayload<CMsgClientLogonResponse>(CMsgClientLogonResponse.ADAPTER).dataNullable?.eresult == EResult.OK.encoded) {
                    startCollector()
                }
            }

            EMsg.k_EMsgClientLoggedOff, EMsg.k_EMsgClientLogOff -> {
                cloudCollector?.cancel()
            }

            else -> Unit
        }
    }

    @JvmInline
    value class Library(private val packed: Pair<Int, List<OwnedGame>>) {
        val count get() = packed.first
        val list get() = packed.second
    }
}