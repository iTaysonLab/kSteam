package bruhcollective.itaysonlab.ksteam.models.library.query

import bruhcollective.itaysonlab.ksteam.models.enums.*

/**
 * Builds a [KsLibraryQuery] that can be used in [bruhcollective.itaysonlab.ksteam.handlers.library.Library].
 */
class KsLibraryQueryBuilder constructor() {
    private var appType: MutableList<ECollectionAppType> = mutableListOf()
    private var playState: ECollectionPlayState? = null
    private var storeCategories: MutableList<List<EStoreCategory>> = mutableListOf()
    private var controllerSupport: KsLibraryQueryControllerSupportFilter = KsLibraryQueryControllerSupportFilter.None
    private var searchQuery: String? = null
    private var offset: Int = 0
    private var limit: Int = 0
    private var ownerTypeFilter: KsLibraryQueryOwnerFilter = KsLibraryQueryOwnerFilter.None
    private var masterSubPackageId: Int = 0
    private var storeTags: MutableList<Int> = mutableListOf()
    private var steamDeckMinimumSupport: ESteamDeckSupport = ESteamDeckSupport.Unknown
    private var sortBy: KsLibraryQuerySortBy = KsLibraryQuerySortBy.Name
    private var sortByDirection: KsLibraryQuerySortByDirection = KsLibraryQuerySortByDirection.Ascending
    private var fetchFullInformation: Boolean = false
    private var alwaysFetchLicenses: Boolean = false
    private var alwaysFetchPlayTime: Boolean = false

    internal constructor(existing: KsLibraryQuery): this() {
        appType = existing.appType.toMutableList()
        playState = existing.playState
        storeCategories = existing.storeCategories.toMutableList()
        controllerSupport = existing.controllerSupport
        searchQuery = existing.searchQuery
        offset = existing.offset
        limit = existing.limit
        ownerTypeFilter = existing.ownerTypeFilter
        masterSubPackageId = existing.masterSubPackageId
        storeTags = existing.storeTags.toMutableList()
        steamDeckMinimumSupport = existing.steamDeckMinimumSupport
        sortBy = existing.sortBy
        sortByDirection = existing.sortByDirection
        fetchFullInformation = existing.fetchFullInformation
    }

    /**
     * Adds [ECollectionAppType] filter.
     *
     * If none was specified - everything will be returned.
     * If several - filter behaves like 'OR' operator, returning only specified types.
     */
    fun withAppType(type: ECollectionAppType) = apply {
        appType.add(type)
    }

    /**
     * Adds a [EStoreCategory] filter.
     *
     * If none was specified - everything will be returned.
     * If several - filter behaves like 'AND' operator between each set, returning apps that match specified categories.
     *
     * This method adds a set of filters that are matched by 'OR' operator:
     * ```kotlin
     * // will include apps that has categories (PS5ControllerSupport) AND (Singleplayer OR Multiplayer)
     * query
     *  .withStoreCategories(EStoreCategory.Singleplayer, EStoreCategory.Multiplayer)
     *  .withStoreCategory(EStoreCategory.PS5ControllerSupport)
     * ```
     */
    fun withStoreCategory(category: EStoreCategory) = apply {
        storeCategories.add(listOf(category))
    }

    /**
     * Adds a set of [EStoreCategory] filters.
     *
     * If none was specified - everything will be returned.
     * If several - filter behaves like 'AND' operator between each set, returning apps that match specified categories.
     *
     * This method adds a set of filters that are matched by 'OR' operator:
     * ```kotlin
     * // will include apps that has categories (PS5ControllerSupport) AND (Singleplayer OR Multiplayer)
     * query
     *  .withStoreCategories(EStoreCategory.Singleplayer, EStoreCategory.Multiplayer)
     *  .withStoreCategory(EStoreCategory.PS5ControllerSupport)
     * ```
     */
    fun withStoreCategories(categories: List<EStoreCategory>) = apply {
        storeCategories.add(categories)
    }

    /**
     * @see withStoreCategories
     */
    fun withStoreCategories(vararg categories: EStoreCategory) = withStoreCategories(categories.toList())

    /**
     * Adds a store tag filter.
     *
     * If none was specified - everything will be returned.
     * If several - filter behaves like 'AND' operator, returning apps that contains ALL of specified tags.
     */
    fun withStoreTag(tag: Int) = apply {
        storeTags.add(tag)
    }

    /**
     * Adds store tag filters.
     *
     * The filter behaves like 'AND' operator, returning apps that contains ALL of specified tags.
     */
    fun withStoreTags(tags: List<Int>) = apply {
        storeTags.addAll(tags)
    }

    /**
     * Adds store tag filters.
     *
     * The filter behaves like 'AND' operator, returning apps that contains ALL of specified tags.
     */
    fun withStoreTags(vararg tags: Int) = apply {
        storeTags.addAll(tags.toList())
    }

    /**
     * Adds a genre filter.
     *
     * The filter behaves like 'AND' operator, returning apps that contains ALL of specified genres.
     */
    fun withGenre(genre: ECollectionGenre) = apply {
        storeTags.add(genre.tagNumber)
    }

    /**
     * Sets [ECollectionPlayState] filter. This runs after the initial query and is intended for multi-account setups or Family Sharing.
     *
     * Only [ECollectionPlayState.PlayedPreviously] and [ECollectionPlayState.PlayedNever] are used in kSteam - other values will be ignored.
     */
    fun withPlayState(state: ECollectionPlayState) = apply {
        playState = state
    }

    /**
     * Sets search query for application name. The search will be case-insensitive.
     */
    fun withSearchQuery(query: String) = apply {
        searchQuery = query
    }

    /**
     * Sets [KsLibraryQueryControllerSupportFilter] filter. Multiple calls will replace the value before [build].
     */
    fun withControllerSupport(state: KsLibraryQueryControllerSupportFilter) = apply {
        controllerSupport = state
    }

    /**
     * Sets a limit. 0 will disable the limit, negative values will result in a [IllegalArgumentException].
     *
     * Please note that limit will be done BEFORE [withPlayState] and [withOwnerFilter] - these filters run on limited results (limit 10 means final filter will be from 10 apps).
     */
    fun withLimit(value: Int) = apply {
        require(value >= 0) { "Limit must be a positive number." }
        limit = value
    }

    /**
     * Sets an offset. 0 will start from the beginning the limit, negative values will result in a [IllegalArgumentException].
     *
     * Please note that offset will be done BEFORE [withPlayState] and [withOwnerFilter] - these filters run on limited results (offset 10 means final filter will be from offset 10 apps).
     */
    fun withOffset(value: Int) = apply {
        require(value >= 0) { "Offset must be a positive number." }
        offset = value
    }

    /**
     * Sets a master subscription package filter. Currently, only `EA Play` is supported with a value of `1289670`.
     */
    fun withMasterSubscriptionPackage(packageId: Int) = apply {
        masterSubPackageId = packageId
    }

    /**
     * Sets an [KsLibraryQueryOwnerFilter] filter. This runs after the initial query and is intended for multi-account setups or Family Sharing.
     */
    fun withOwnerFilter(value: KsLibraryQueryOwnerFilter) = apply {
        ownerTypeFilter = value
    }

    /**
     * Sets a minimum [ESteamDeckSupport] level.
     */
    fun withSteamDeckMinimumSupport(value: ESteamDeckSupport) = apply {
        steamDeckMinimumSupport = value
    }

    /**
     * Sets if this query must query for ALL data in database. This will add the following data in [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication] objects:
     * - [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.contentDescriptors]
     * - [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.tags]
     * - [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.categories]
     * - [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.Assets.localizedAssets]
     * - [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.developers]
     * - [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.publishers]
     * - [bruhcollective.itaysonlab.ksteam.models.app.SteamApplication.franchises]
     *
     * **IMPORTANT**: For large query results, this will SIGNIFICANTLY increase query time!
     * For example, querying 1507 apps on Samsung Galaxy S23 takes approximately 4 seconds with this setting enabled...
     *
     * Note that you don't need this to be turned on in order to use any of the sort/filter options in queries. For example, you still can filter by tags or categories even with [fetchFullInformation] being false.
     */
    fun fetchFullInformation(value: Boolean) = apply {
        fetchFullInformation = value
    }

    /**
     * If true, this query will always populate [bruhcollective.itaysonlab.ksteam.models.app.OwnedSteamApplication.licenses].
     *
     * Note that this value will be ignored if [KsLibraryQueryOwnerFilter] is set to any value except for [KsLibraryQueryOwnerFilter.None].
     * In this case, licenses will always be returned.
     */
    fun alwaysFetchLicenses(value: Boolean) = apply {
        alwaysFetchLicenses = value
    }

    /**
     * If true, this query will always populate [bruhcollective.itaysonlab.ksteam.models.app.OwnedSteamApplication.playTime].
     *
     * Note that this value will be ignored if [ECollectionPlayState] is set to any value.
     * In this case, play time will always be returned.
     */
    fun alwaysFetchPlayTime(value: Boolean) = apply {
        alwaysFetchPlayTime = value
    }

    /**
     * Sets a sorting direction and order.
     */
    fun sortBy(order: KsLibraryQuerySortBy, direction: KsLibraryQuerySortByDirection) = apply {
        sortBy = order
        sortByDirection = direction
    }

    /**
     * Builds a [KsLibraryQuery] with specified parameters.
     */
    fun build(): KsLibraryQuery {
        return KsLibraryQuery(
            appType = appType,
            playState = playState,
            storeCategories = storeCategories,
            controllerSupport = controllerSupport,
            searchQuery = searchQuery,
            limit = limit,
            offset = offset,
            ownerTypeFilter = ownerTypeFilter,
            masterSubPackageId = masterSubPackageId,
            storeTags = storeTags,
            steamDeckMinimumSupport = steamDeckMinimumSupport,
            sortBy = sortBy,
            sortByDirection = sortByDirection,
            fetchFullInformation = fetchFullInformation,
            alwaysFetchLicenses = alwaysFetchLicenses,
            alwaysFetchPlayTime = alwaysFetchPlayTime,
        )
    }
}