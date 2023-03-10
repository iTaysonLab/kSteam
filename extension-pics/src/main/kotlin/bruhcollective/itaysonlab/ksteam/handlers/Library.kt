package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.debug.logError
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EPlayState
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.models.library.LibraryCollection
import bruhcollective.itaysonlab.ksteam.models.library.LibraryShelf
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.platform.CreateSupervisedCoroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import steam.webui.cloudconfigstore.CCloudConfigStore_Entry
import steam.webui.common.CMsgClientLogonResponse
import steam.webui.player.*
import kotlin.time.Duration.Companion.minutes

/**
 * Provides access to user's owned app library.
 */
class Library(
    private val steamClient: SteamClient
) : BaseHandler {
    private val scope = CreateSupervisedCoroutineScope("LibraryCollector", Dispatchers.Default)

    private var cloudCollector: Job? = null
    private var userPlayTimesCollector: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    //
    private val _isLoadingLibrary = MutableStateFlow(false)
    val isLoadingLibrary = _isLoadingLibrary.asStateFlow()

    private val _isLoadingPlayTimes = MutableStateFlow(false)
    val isLoadingPlayTimes = _isLoadingPlayTimes.asStateFlow()
    //

    private val _userCollections = MutableStateFlow<List<LibraryCollection>>(emptyList())
    val userCollections = _userCollections.asStateFlow()

    private val _favoriteCollection = MutableStateFlow<LibraryCollection?>(null)
    val favoriteCollection = _favoriteCollection.asStateFlow()

    private val _hiddenCollection = MutableStateFlow<LibraryCollection?>(null)
    val hiddenCollection = _hiddenCollection.asStateFlow()

    private val _shelves = MutableStateFlow<List<LibraryShelf>>(emptyList())
    val shelves = _shelves.asStateFlow()

    private val _playtime = MutableStateFlow<Map<AppId, CPlayer_GetLastPlayedTimes_Response_Game>>(emptyMap())
    val playtime = _playtime.asStateFlow()

    /**
     * Queries eligible apps in a collection by its [id].
     *
     * @return a [Flow] of [AppInfo] which is changed by collection editing
     */
    fun getAppsInCollection(id: String, limit: Int = 0): Flow<List<AppSummary>> {
        return userCollections.mapNotNull { list ->
            list.firstOrNull { it.id == id }
        }.map { collection ->
            val collectionFilters = collection.filterSpec?.parseFilters()
            val hasPlayState = collectionFilters?.byPlayState?.entries?.contains(EPlayState.PlayedNever) == true || collectionFilters?.byPlayState?.entries?.contains(
                EPlayState.PlayedPreviously) == true

            val playTime = if (hasPlayState) {
                _playtime.first() // Await playtime
            } else {
                null
            }

            collectionFilters?.let { filters ->
                steamClient.pics.getAppSummariesFiltered(filters, limit).let { appInfoList ->
                    if (hasPlayState) {
                        val neverPlayed = filters.byPlayState.entries.contains(EPlayState.PlayedNever)

                        appInfoList.filter {
                            if (neverPlayed) {
                                (playTime?.get(it.id)?.first_playtime ?: 0) == 0
                            } else {
                                (playTime?.get(it.id)?.first_playtime ?: 0) != 0
                            }
                        }
                    } else {
                        appInfoList
                    }
                }
            } ?: steamClient.pics.getAppSummariesByAppId(collection.added, limit).values.sortedBy { it.name }
        }
    }

    /**
     * Queries a collection by its [id].
     */
    fun getCollection(id: String): Flow<LibraryCollection> {
        return userCollections.mapNotNull { list ->
            list.firstOrNull { it.id == id }
        }
    }

    fun getRecentApps(): Flow<List<AppSummary>> {
        return _playtime.map {
            it.values.sortedByDescending { a -> a.last_playtime ?: 0 }.take(5).mapNotNull { a -> AppId(a.appid ?: return@mapNotNull null) }
        }.map {
            steamClient.pics.getAppSummariesByAppId(it).values.sortedBy { a -> a.name }
        }
    }

    fun getFavoriteApps(limit: Int = 0): Flow<List<AppSummary>> {
        return getAppsInCollection("favorite", limit)
    }

    /**
     * Edit a collection.
     *
     * This will trigger [collections] change.
     */
    suspend fun editCollection() {

    }

    /**
     * Creates a new collection.
     *
     * This will trigger [collections] change.
     */
    suspend fun createCollection() {

    }

    /**
     * Deletes a collection.
     *
     * This will trigger [collections] change.
     */
    suspend fun deleteCollection(id: String) {

    }

    /**
     * Checks if a current user is actually owning an [appId].
     */
    fun ownsThisApp(appId: AppId) = steamClient.pics.appIds.contains(appId)

    private suspend fun startCollector() {
        // Collect user play time

        if (userPlayTimesCollector == null || userPlayTimesCollector?.isCompleted == true) {
            userPlayTimesCollector = scope.launch {
                _isLoadingPlayTimes.value = true

                while (true) {
                    logDebug("Library:Collector", "Requesting last played times")

                    _playtime.update {
                        steamClient.unifiedMessages.execute(
                            methodName = "Player.ClientGetLastPlayedTimes",
                            requestAdapter = CPlayer_GetLastPlayedTimes_Request.ADAPTER,
                            responseAdapter = CPlayer_GetLastPlayedTimes_Response.ADAPTER,
                            requestData = CPlayer_GetLastPlayedTimes_Request()
                        ).dataNullable?.games?.associateBy { AppId(it.appid ?: 0) }.orEmpty()
                    }

                    _isLoadingPlayTimes.value = false

                    delay(30.minutes) // Steam client approximately requests this data every 30 minutes
                }
            }
        }

        // Collect WebUI info (if collector is not started)

        // Wait until PICS data is loaded (avoids UI lockup/race condition when PICS is being loaded lately than folders and Flows won't update)
        steamClient.pics.isPicsAvailable.first { it == Pics.PicsState.Ready }

        if (cloudCollector == null || cloudCollector?.isCompleted == true) {
            cloudCollector = steamClient.cloudConfiguration.request(1).onEach { list ->
                handleUserLibrary(list)
            }.onCompletion {
                cloudCollector = null

                if (it != null && it !is CancellationException) {
                    logError("Library:Collector", "Error occurred when collecting library data: ${it.message}")
                    it.printStackTrace()

                    delay(1000L)

                    startCollector()
                }
            }.launchIn(scope)
        }
    }

    private fun handleUserLibrary(entries: List<CCloudConfigStore_Entry>) {
        _isLoadingLibrary.value = false

        entries.forEach { entry ->
            logVerbose("Library:Cloud", entry.toString())
        }

        // -- User Collections --
        entries.asSequence().filterNot { it.is_deleted == true }.filter { it.key.orEmpty().startsWith("user-collections") }.mapNotNull {
            val entry = json.decodeFromString<LibraryCollection.CollectionModel>(it.value_ ?: return@mapNotNull null)

            LibraryCollection(
                id = entry.id,
                name = entry.name,
                added = entry.added.map(::AppId),
                removed = entry.removed.map(::AppId),
                filterSpec = entry.filterSpec,
                timestamp = it.timestamp ?: 0,
                version = it.version ?: 0
            ).also { c ->
                logVerbose("Library:Collection", c.toString())
            }
        }.filter {
            when (it.id) {
                "favorite" -> {
                    _favoriteCollection.value = it
                    false
                }

                "hidden" -> {
                    _hiddenCollection.value = it
                    false
                }

                else -> {
                    true
                }
            }
        }.sortedWith { o1, o2 ->
            o1.name.compareTo(o2.name, ignoreCase = true)
        }.toList().let { collections ->
            _userCollections.value = collections
        }

        entries.asSequence().filterNot { it.is_deleted == true }.filter { it.key.orEmpty().startsWith("showcase") }.mapNotNull {
            val entry = json.decodeFromString<LibraryShelf.LibraryShelfRemote>(it.value_ ?: return@mapNotNull null)

            LibraryShelf(
                id = it.key ?: return@mapNotNull null,
                linkedCollection = entry.linkedCollection,
                sortBy = entry.sortBy,
                lastChangedMs = entry.lastChangedMs,
                orderTimestamp = entry.orderTimestamp ?: 0L,
                version = it.version ?: 0,
                remoteTimestamp = it.timestamp ?: 0
            )
        }.filter {
            it.linkedCollection.isNotEmpty()
        }.sortedByDescending {
            if (it.orderTimestamp != 0L) {
                it.orderTimestamp
            } else {
                it.id.removePrefix("showcases.").toLongOrNull() ?: 0L
            }
        }.toList().let { shelves ->
            _shelves.value = shelves
        }

        _isLoadingLibrary.value = true
    }

    /**
     * Awaits until PICS is ready and library is loaded.
     */
    private suspend fun awaitInfrastructure() {
        _isLoadingLibrary.first { it }
        steamClient.pics.isPicsAvailable.first { it == Pics.PicsState.Ready }
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientLogOnResponse -> {
                if (packet.getProtoPayload(CMsgClientLogonResponse.ADAPTER).dataNullable?.eresult == EResult.OK.encoded) {
                    startCollector()
                }
            }

            EMsg.k_EMsgClientLoggedOff, EMsg.k_EMsgClientLogOff -> {
                cloudCollector?.cancel()
                userPlayTimesCollector?.cancel()
            }

            else -> Unit
        }
    }
}