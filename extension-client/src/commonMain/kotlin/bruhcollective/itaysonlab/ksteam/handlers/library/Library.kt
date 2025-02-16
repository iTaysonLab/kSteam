package bruhcollective.itaysonlab.ksteam.handlers.library

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.database.sql.compileKsLibraryQueryToSql
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.app.OwnedSteamApplication
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplicationFactory
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplicationPlaytime
import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionPlayState
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.models.library.LibraryCollection
import bruhcollective.itaysonlab.ksteam.models.library.LibraryShelf
import bruhcollective.itaysonlab.ksteam.models.library.OwnedGame
import bruhcollective.itaysonlab.ksteam.models.library.RemoteCollectionModel
import bruhcollective.itaysonlab.ksteam.models.library.query.*
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.util.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.util.executeSteamOrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import steam.webui.cloudconfigstore.CCloudConfigStore_Entry
import steam.webui.common.CMsgClientLogonResponse
import steam.webui.player.CPlayer_GetLastPlayedTimes_Request
import steam.webui.player.CPlayer_GetLastPlayedTimes_Response_Game
import steam.webui.player.CPlayer_LastPlayedTimes_Notification
import kotlin.time.Duration.Companion.minutes

/**
 * Provides access to user's owned app library.
 */
class Library internal constructor(
    internal val steamClient: ExtendedSteamClient
) {
    companion object {
        private const val TAG = "LibraryHandler"
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

    private val _ownedGames = MutableStateFlow<Map<Int, OwnedGame>>(emptyMap())
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

    private val _playtime = MutableStateFlow<Map<Int, CPlayer_GetLastPlayedTimes_Response_Game>>(emptyMap())
    val playtime = _playtime.asStateFlow()

    /**
     * Queries apps from a collection by its [id].
     *
     * @return a [Flow] of [OwnedSteamApplication] which is changed by collection editing
     */
    fun getAppsInCollection(id: String, full: Boolean, limit: Int = 0): Flow<List<OwnedSteamApplication>> {
        val collectionFlow = when (id) {
            FavoriteCollection -> favoriteCollection
            HiddenCollection -> hiddenCollection
            else -> getCollection(id)
        }

        return collectionFlow.map { collection ->
            getAppsInCollection(collection, full, limit)
        }
    }

    fun getAppsInCollection(
        collectionFlow: Flow<LibraryCollection>,
        full: Boolean,
        limit: Int = 0
    ): Flow<List<OwnedSteamApplication>> = collectionFlow.map { collection ->
        getAppsInCollection(collection, full, limit)
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
    suspend fun getAppsInCollection(
        collection: LibraryCollection,
        full: Boolean,
        limit: Int = 0
    ): List<OwnedSteamApplication> {
        return when (collection) {
            is LibraryCollection.Simple -> {
                // Filter out non-Steam games that can be accidentally added to cloud Steam collections
                steamClient.pics.getSteamApplications(
                    full = full,
                    collection.added.filter { it > 0 && it < Int.MAX_VALUE }.map(Long::toInt)
                )
                    .let {
                        if (limit > 0) {
                            it.take(limit)
                        } else {
                            it
                        }
                    }
                    .map { app -> augmentSteamApplication(app) }
            }

            is LibraryCollection.Dynamic -> {
                execute(
                    collection.toKsLibraryQuery().newBuilder().withLimit(limit).fetchFullInformation(full)
                        .alwaysFetchPlayTime(full).alwaysFetchLicenses(full).build()
                )
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
    fun getRecentApps(full: Boolean): Flow<List<OwnedSteamApplication>> {
        return _playtime.map {
            it.values.asSequence().sortedByDescending { a -> a.last_playtime ?: 0 }.mapNotNull { a -> a.appid }.take(5)
                .toList()
        }.map { ids ->
            steamClient.pics.getSteamApplications(full, ids).map { app -> augmentSteamApplication(app) }
        }
    }

    fun getFavoriteApps(full: Boolean, limit: Int = 0): Flow<List<OwnedSteamApplication>> {
        return getAppsInCollection(favoriteCollection, full, limit)
    }

    fun getHiddenApps(full: Boolean, limit: Int = 0): Flow<List<OwnedSteamApplication>> {
        return getAppsInCollection(hiddenCollection, full, limit)
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
    suspend fun ownsThisApp(appId: AppId): Boolean {
        return steamClient.pics.findLicensesForCurrentUser(appId).isNotEmpty()
    }

    private suspend fun startCollector() {
        // Collect user play time

        if (userPlayTimesCollector == null || userPlayTimesCollector?.isCompleted == true) {
            userPlayTimesCollector = scope.launch {
                _isLoadingPlayTimes.value = true

                while (true) {
                    steamClient.logger.logDebug("Library:Collector") { "Requesting last played times" }

                    _playtime.update {
                        steamClient.grpc.player.ClientGetLastPlayedTimes().executeSteamOrNull(
                            data = CPlayer_GetLastPlayedTimes_Request()
                        )?.games?.associateBy { it.appid ?: 0 }.orEmpty()
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
                    steamClient.logger.logError("Library:Collector") { "Error occurred when collecting library data: ${it.message}" }
                    it.printStackTrace()

                    delay(1000L)

                    startCollector()
                }
            }.launchIn(scope)
        }
    }

    private fun handleUserLibrary(entries: List<CCloudConfigStore_Entry>) {
        _isLoadingLibrary.value = true

        if (steamClient.logger.enableVerboseLogs) {
            steamClient.logger.logVerbose("Library:Cloud") { "Printing cloud keys:" }

            entries.forEach { entry ->
                steamClient.logger.logVerbose("Library:Cloud") { entry.toString() }
            }
        }

        // -- User Collections --
        entries.asSequence().filterNot { it.is_deleted == true }
            .filter { it.key.orEmpty().startsWith("user-collections") }.mapNotNull { entry ->
            val entryObject = try {
                json.decodeFromString<RemoteCollectionModel>(entry.value_ ?: return@mapNotNull null)
            } catch (e: Exception) {
                steamClient.logger.logError("Library:Collection") { entry.value_.toString() }
                e.printStackTrace()
                return@mapNotNull null
            }

            LibraryCollection.fromJsonCollection(
                entryObject,
                entry.timestamp ?: return@mapNotNull null,
                entry.version ?: return@mapNotNull null
            ).also { c ->
                steamClient.logger.logVerbose("Library:Collection") { c.toString() }
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

        // -- User Shelves --
        entries.asSequence().filterNot { it.is_deleted == true }.filter { it.key.orEmpty().startsWith("showcase") }
            .mapNotNull {
                val entry = json.decodeFromString<LibraryShelf.LibraryShelfRemote>(it.value_ ?: return@mapNotNull null)

                LibraryShelf(
                    id = it.key ?: return@mapNotNull null,
                    linkedCollection = entry.linkedCollection,
                    sortBy = entry.sortBy,
                    lastChangedMs = entry.lastChangedMs,
                    orderTimestamp = entry.orderTimestamp ?: 0.0,
                    version = it.version ?: 0,
                    remoteTimestamp = it.timestamp ?: 0
                )
            }.filter {
            it.linkedCollection.isNotEmpty()
        }.sortedByDescending {
            if (it.orderTimestamp != 0.0) {
                it.orderTimestamp
            } else {
                it.id.removePrefix("showcases.").toDoubleOrNull() ?: 0.0
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
        steamClient.pics.awaitPicsInitialization()
    }

    private suspend fun requestOwnedGames() {
        _ownedGames.value = steamClient.player.getOwnedGames().associateBy { it.id }
    }

    init {
        steamClient.on(EMsg.k_EMsgClientLogOnResponse) { packet ->
            if (packet.isProtobuf() && CMsgClientLogonResponse.ADAPTER.decode(packet.payload).eresult == EResult.OK.encoded) {
                startCollector()
            }
        }

        steamClient.on(EMsg.k_EMsgClientLicenseList) { packet ->
            requestOwnedGames()
        }

        steamClient.on(EMsg.k_EMsgClientLoggedOff) { packet ->
            cloudCollector?.cancel()
            userPlayTimesCollector?.cancel()
        }

        steamClient.onTypedRpc(
            "PlayerClient.NotifyLastPlayedTimes#1",
            CPlayer_LastPlayedTimes_Notification.ADAPTER
        ) { notification ->
            steamClient.logger.logDebug(TAG) { "Received last played times notification: $notification" }

            _playtime.update {
                it.toMutableMap().apply {
                    notification.games.forEach { lastPlayedGame ->
                        this[lastPlayedGame.appid ?: return@forEach] = lastPlayedGame
                    }
                }
            }
        }
    }

    // region Library Query

    /**
     * Returns [SteamApplicationPlaytime].
     */
    fun getApplicationPlaytime(appId: AppId): SteamApplicationPlaytime? {
        return _playtime.value[appId.value]?.let { playTime ->
            SteamApplicationPlaytime(
                firstLaunch = SteamApplicationPlaytime.PlatformTimes(
                    total = playTime.first_playtime ?: 0,
                    deck = playTime.first_deck_playtime ?: 0,
                    windows = playTime.first_windows_playtime ?: 0,
                    linux = playTime.first_linux_playtime ?: 0,
                    mac = playTime.first_mac_playtime ?: 0
                ), lastLaunch = SteamApplicationPlaytime.PlatformTimes(
                    total = playTime.last_playtime ?: 0,
                    deck = playTime.last_deck_playtime ?: 0,
                    windows = playTime.last_windows_playtime ?: 0,
                    linux = playTime.last_linux_playtime ?: 0,
                    mac = playTime.last_mac_playtime ?: 0
                ), playTime = SteamApplicationPlaytime.PlatformTimes(
                    total = playTime.playtime_disconnected ?: 0,
                    deck = playTime.playtime_deck_forever ?: 0,
                    windows = playTime.playtime_windows_forever ?: 0,
                    linux = playTime.playtime_linux_forever ?: 0,
                    mac = playTime.playtime_windows_forever ?: 0
                )
            )
        }
    }

    /**
     * Augments [SteamApplication] with playtime and license information.
     */
    suspend fun augmentSteamApplication(application: SteamApplication): OwnedSteamApplication {
        return OwnedSteamApplication(
            application = application,
            licenses = steamClient.pics.findLicensesForCurrentUser(application.id),
            playTime = getApplicationPlaytime(application.id)
        )
    }

    /**
     * Executes [KsLibraryQuery] in a separate thread and returns a list of OwnedSteamApplication.
     *
     * @param query a compiled kSteam Library Query, use [KsLibraryQueryBuilder] to build them
     * @return a [kotlinx.coroutines.flow.Flow] of [OwnedSteamApplication]
     */
    suspend fun execute(query: KsLibraryQuery): List<OwnedSteamApplication> {
        val sql = compileKsLibraryQueryToSql(query)

        steamClient.logger.logVerbose(TAG) { sql.sql }

        val initialQueryResults = if (query.fetchFullInformation) {
            steamClient.database.sharedDatabase.picsApplications().rawFilteredApplicationsFull(sql)
                .map(SteamApplicationFactory::fromDatabase)
        } else {
            steamClient.database.sharedDatabase.picsApplications().rawFilteredApplications(sql)
                .map(SteamApplicationFactory::fromDatabase)
        }

        // Augment with license and playtime information
        var ownedSteamApplications = initialQueryResults.map { steamApplication ->
            OwnedSteamApplication(
                application = steamApplication,
                licenses = if (query.alwaysFetchLicenses || query.ownerTypeFilter != KsLibraryQueryOwnerFilter.None) {
                    steamClient.pics.findLicensesForCurrentUser(steamApplication.id)
                } else {
                    emptyList()
                },
                playTime = if (query.alwaysFetchPlayTime || query.playState == ECollectionPlayState.PlayedNever || query.playState == ECollectionPlayState.PlayedPreviously || query.sortBy == KsLibraryQuerySortBy.PlayedTime || query.sortBy == KsLibraryQuerySortBy.LastPlayed) {
                    steamClient.library.getApplicationPlaytime(steamApplication.id)
                } else {
                    null
                }
            )
        }

        // Filter by Play State
        when (query.playState) {
            ECollectionPlayState.PlayedNever -> {
                ownedSteamApplications = ownedSteamApplications.filter {
                    it.playTime != null && it.playTime.firstLaunch.total == 0
                }
            }

            ECollectionPlayState.PlayedPreviously -> {
                ownedSteamApplications = ownedSteamApplications.filter {
                    it.playTime != null && it.playTime.firstLaunch.total != 0
                }
            }

            else -> {}
        }

        // Sort by Play Time
        if (query.sortBy == KsLibraryQuerySortBy.PlayedTime) {
            ownedSteamApplications = when (query.sortByDirection) {
                KsLibraryQuerySortByDirection.Ascending -> {
                    ownedSteamApplications.sortedBy {
                        it.playTime?.playTime?.total ?: 0
                    }
                }

                KsLibraryQuerySortByDirection.Descending -> {
                    ownedSteamApplications.sortedByDescending {
                        it.playTime?.playTime?.total ?: 0
                    }
                }
            }
        } else if (query.sortBy == KsLibraryQuerySortBy.LastPlayed) {
            ownedSteamApplications = when (query.sortByDirection) {
                KsLibraryQuerySortByDirection.Ascending -> {
                    ownedSteamApplications.sortedBy {
                        it.playTime?.lastLaunch?.total ?: 0
                    }
                }

                KsLibraryQuerySortByDirection.Descending -> {
                    ownedSteamApplications.sortedByDescending {
                        it.playTime?.lastLaunch?.total ?: 0
                    }
                }
            }
        }

        // And, finally, filter by owner
        return when (query.ownerTypeFilter) {
            KsLibraryQueryOwnerFilter.None -> ownedSteamApplications
            KsLibraryQueryOwnerFilter.Default -> ownedSteamApplications.filter { it.licenses.isNotEmpty() }
            KsLibraryQueryOwnerFilter.OwnedOnly -> ownedSteamApplications.filter { it.ownsThisApp(steamClient) }
        }
    }

    // endregion
}