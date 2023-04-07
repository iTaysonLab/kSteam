package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EPlayState
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.models.library.LibraryCollection
import bruhcollective.itaysonlab.ksteam.models.library.LibraryShelf
import bruhcollective.itaysonlab.ksteam.models.library.OwnedGame
import bruhcollective.itaysonlab.ksteam.models.library.RemoteCollectionModel
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.util.CreateSupervisedCoroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import steam.webui.cloudconfigstore.CCloudConfigStore_Entry
import steam.webui.common.CMsgClientLogonResponse
import steam.webui.player.CPlayer_GetLastPlayedTimes_Request
import steam.webui.player.CPlayer_GetLastPlayedTimes_Response
import steam.webui.player.CPlayer_GetLastPlayedTimes_Response_Game
import kotlin.time.Duration.Companion.minutes

/**
 * Provides access to user's owned app library.
 */
class Library(
    private val steamClient: SteamClient
) : BaseHandler {
    companion object {
        const val FavoriteCollection = "favorite"
        const val HiddenCollection = "hidden"
    }

    private val scope = CreateSupervisedCoroutineScope("LibraryCollector", Dispatchers.Default)

    private var cloudCollector: Job? = null
    private var userPlayTimesCollector: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    //
    private val _isLoadingLibrary = MutableStateFlow(false)
    val isLoadingLibrary = _isLoadingLibrary.asStateFlow()

    private val _isLoadingPlayTimes = MutableStateFlow(false)
    val isLoadingPlayTimes = _isLoadingPlayTimes.asStateFlow()

    private val _ownedGames = MutableStateFlow<Map<AppId, OwnedGame>>(emptyMap())
    val ownedGames = _ownedGames.asStateFlow()

    //

    private val _userCollections = MutableStateFlow<Map<String, LibraryCollection>>(emptyMap())
    val userCollections = _userCollections.asStateFlow()

    private val _favoriteCollection = MutableStateFlow<LibraryCollection>(LibraryCollection.Placeholder)
    val favoriteCollection = _favoriteCollection.asStateFlow()

    private val _hiddenCollection = MutableStateFlow<LibraryCollection>(LibraryCollection.Placeholder)
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
    fun getAppsInCollection(id: String, limit: Int = 0): Flow<List<AppSummary>> = userCollections.mapNotNull { collections ->
        getAppsInCollection(collections[id] ?: return@mapNotNull null).takeIfNotZero(limit).toList()
    }

    fun getAppsInCollection(collectionFlow: Flow<LibraryCollection>, limit: Int = 0): Flow<List<AppSummary>> = collectionFlow.map { collection ->
        getAppsInCollection(collection).takeIfNotZero(limit).toList()
    }

    /**
     * Queries eligible owned apps in a collection by its [id].
     *
     * Recommended to use in UI apps because of automatically updating Flow based on both collection info and owned games metadata
     *
     * @param id collection ID
     * @param limit how many items to show, default is 0 which means "everything"
     * @return a [Flow] of [OwnedGame] which is changed by collection editing
     */
    fun getOwnedAppsInCollection(id: String, limit: Int = 0): Flow<List<OwnedGame>> {
        val collectionFlow = getCollection(id)

        return collectionFlow.combine(ownedGames) { collection, ownedMap ->
            getAppsInCollection(collection).mapNotNull { summary ->
                ownedMap[summary.id]
            }.takeIfNotZero(limit).toList()
        }
    }

    /**
     * Queries all owned apps of the current connected account.
     *
     * @param limit how many items to show, default is 0 which means "everything"
     * @return a [Flow] of [OwnedGame] which is changed by collection editing
     */
    fun getAllOwnedApps(limit: Int = 0): Flow<Collection<OwnedGame>> {
        return ownedGames.map {
            it.values
        }
    }

    /**
     * Queries eligible apps in a collection.
     *
     * @return a list of [AppInfo]
     */
    suspend fun getAppsInCollection(collection: LibraryCollection): Sequence<AppSummary> {
        return when (collection) {
            is LibraryCollection.Simple -> {
                steamClient.pics.getAppSummariesByAppId(collection.added).values.asSequence().sortedBy { it.name }
            }

            is LibraryCollection.Dynamic -> {
                val hasPlayStateNeverPlayed = collection.filters.byPlayState.entries.contains(EPlayState.PlayedNever)
                val hasPlayStatePlayedPreviously = collection.filters.byPlayState.entries.contains(EPlayState.PlayedPreviously)

                val playTime = if (hasPlayStateNeverPlayed || hasPlayStatePlayedPreviously) {
                    _playtime.first() // Await playtime
                } else {
                    null
                }

                steamClient.pics.getAppSummariesFiltered(collection.filters).let { appInfoList ->
                    when {
                        hasPlayStateNeverPlayed -> {
                            appInfoList.filter {
                                (playTime?.get(it.id)?.first_playtime ?: 0) == 0
                            }
                        }

                        hasPlayStatePlayedPreviously -> {
                            appInfoList.filter {
                                (playTime?.get(it.id)?.first_playtime ?: 0) != 0
                            }
                        }

                        else -> appInfoList
                    }
                }
            }
        }
    }

    /**
     * Queries a collection by its [id].
     */
    fun getCollection(id: String): Flow<LibraryCollection> {
        return userCollections.mapNotNull { list ->
            list[id]
        }
    }

    /**
     * Fetch a live-updated (every 30 minutes) list of at max 5 games, sorted by last launch date.
     */
    fun getRecentApps(): Flow<List<AppSummary>> {
        return _playtime.map {
            it.values.asSequence().sortedByDescending { a -> a.last_playtime ?: 0 }.mapNotNull { a -> AppId(a.appid ?: return@mapNotNull null) }.take(5)
        }.map {
            steamClient.pics.getAppSummariesByAppId(it.toList()).values.sortedBy { a -> a.name }
        }
    }

    fun getFavoriteApps(limit: Int = 0): Flow<List<AppSummary>> {
        return getAppsInCollection(favoriteCollection, limit)
    }

    fun getHiddenApps(limit: Int = 0): Flow<List<AppSummary>> {
        return getAppsInCollection(hiddenCollection, limit)
    }

    /**
     * Edit a collection.
     *
     * This will trigger [userCollections] change.
     */
    suspend fun editCollection() {

    }

    /**
     * Creates a new collection.
     *
     * This will trigger [userCollections] change.
     */
    suspend fun createCollection(collection: LibraryCollection) {

    }

    /**
     * Deletes a collection.
     *
     * This will trigger [userCollections] change.
     */
    suspend fun deleteCollection(id: String) {

    }

    /**
     * Checks if a current user is actually owning an [appId].
     */
    fun ownsThisApp(appId: AppId) = steamClient.pics.appIds.contains(appId.id)

    private suspend fun startCollector() {
        // Collect user play time

        if (userPlayTimesCollector == null || userPlayTimesCollector?.isCompleted == true) {
            userPlayTimesCollector = scope.launch {
                _isLoadingPlayTimes.value = true

                while (true) {
                    KSteamLogging.logDebug("Library:Collector", "Requesting last played times")

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

        if (cloudCollector == null || cloudCollector?.isCompleted == true) {
            cloudCollector = steamClient.cloudConfiguration.request(1).onEach { list ->
                handleUserLibrary(list)
            }.onCompletion {
                cloudCollector = null

                if (it != null && it !is CancellationException) {
                    KSteamLogging.logError("Library:Collector", "Error occurred when collecting library data: ${it.message}")
                    it.printStackTrace()

                    delay(1000L)

                    startCollector()
                }
            }.launchIn(scope)
        }
    }

    private fun handleUserLibrary(entries: List<CCloudConfigStore_Entry>) {
        _isLoadingLibrary.value = true

        if (KSteamLogging.enableVerboseLogs) {
            KSteamLogging.logVerbose("Library:Cloud", "Printing cloud keys:")

            entries.forEach { entry ->
                KSteamLogging.logVerbose("Library:Cloud", entry.toString())
            }
        }

        // -- User Collections --
        entries.asSequence().filterNot { it.is_deleted == true }.filter { it.key.orEmpty().startsWith("user-collections") }.mapNotNull { entry ->
            val entryObject = json.decodeFromString<RemoteCollectionModel>(entry.value_ ?: return@mapNotNull null)

            LibraryCollection.fromJsonCollection(entryObject, entry.timestamp ?: return@mapNotNull null, entry.version ?: return@mapNotNull null).also { c ->
                KSteamLogging.logVerbose("Library:Collection", c.toString())
            }
        }.filter {
            when (it.id) {
                FavoriteCollection -> {
                    _favoriteCollection.value = it
                    false
                }

                HiddenCollection -> {
                    _hiddenCollection.value = it
                    false
                }

                else -> {
                    true
                }
            }
        }.associateBy(LibraryCollection::id).let { collections ->
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

        _isLoadingLibrary.value = false
    }

    /**
     * Waits until PICS is ready and library is loaded.
     */
    private suspend fun awaitInfrastructure() {
        _isLoadingLibrary.first { it }
        steamClient.pics.isPicsAvailable.first { it == Pics.PicsState.Ready }
    }

    private suspend fun requestOwnedGames() {
        _ownedGames.value = steamClient.player.getOwnedGames().associateBy { it.id }
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientLogOnResponse -> {
                if (packet.getProtoPayload(CMsgClientLogonResponse.ADAPTER).dataNullable?.eresult == EResult.OK.encoded) {
                    startCollector()
                }
            }

            EMsg.k_EMsgClientLicenseList -> {
                requestOwnedGames()
            }

            EMsg.k_EMsgClientLoggedOff, EMsg.k_EMsgClientLogOff -> {
                cloudCollector?.cancel()
                userPlayTimesCollector?.cancel()
            }

            else -> Unit
        }
    }
}

internal fun <T> Sequence<T>.takeIfNotZero(n: Int): Sequence<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return when (n) {
        0 -> this
        else -> take(n)
    }
}