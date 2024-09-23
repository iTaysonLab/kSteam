package bruhcollective.itaysonlab.ksteam.models.library.query

import bruhcollective.itaysonlab.ksteam.models.enums.EAppType
import bruhcollective.itaysonlab.ksteam.models.enums.EGenre
import bruhcollective.itaysonlab.ksteam.models.enums.EPlayState
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory

/**
 * Builds a [KsLibraryQuery] that can be used in [bruhcollective.itaysonlab.ksteam.handlers.library.Library].
 */
class KsLibraryQueryBuilder constructor() {
    private var appType: MutableList<EAppType> = mutableListOf()
    private var playState: EPlayState? = null
    private var storeCategories: MutableList<List<EStoreCategory>> = mutableListOf()
    private var controllerSupport: KsLibraryQueryControllerSupportFilter = KsLibraryQueryControllerSupportFilter.None
    private var searchQuery: String? = null
    private var limit: Int = 0
    private var ownerTypeFilter: KsLibraryQueryOwnerFilter = KsLibraryQueryOwnerFilter.None
    private var masterSubPackageId: Int = 0
    private var storeTags: MutableList<Int> = mutableListOf()
    private var steamDeckMinimumSupport: ESteamDeckSupport = ESteamDeckSupport.Unknown
    private var sortBy: KsLibraryQuerySortBy = KsLibraryQuerySortBy.Name
    private var sortByDirection: KsLibraryQuerySortByDirection = KsLibraryQuerySortByDirection.Ascending

    internal constructor(existing: KsLibraryQuery): this() {
        appType = existing.appType.toMutableList()
        playState = existing.playState
        storeCategories = existing.storeCategories.toMutableList()
        controllerSupport = existing.controllerSupport
        searchQuery = existing.searchQuery
        limit = existing.limit
        ownerTypeFilter = existing.ownerTypeFilter
        masterSubPackageId = existing.masterSubPackageId
        storeTags = existing.storeTags.toMutableList()
        steamDeckMinimumSupport = existing.steamDeckMinimumSupport
        sortBy = existing.sortBy
        sortByDirection = existing.sortByDirection
    }

    /**
     * Adds [EAppType] filter.
     *
     * If none was specified - everything will be returned.
     * If several - filter behaves like 'OR' operator, returning only specified types.
     */
    fun withAppType(type: EAppType) = apply {
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
    fun withGenre(genre: EGenre) = apply {
        storeTags.add(genre.tagNumber)
    }

    /**
     * Sets [EPlayState] filter. This runs after the initial query and is intended for multi-account setups or Family Sharing.
     *
     * Only [EPlayState.PlayedPreviously] and [EPlayState.PlayedNever] are used in kSteam - other values will be ignored.
     */
    fun withPlayState(state: EPlayState) = apply {
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
            ownerTypeFilter = ownerTypeFilter,
            masterSubPackageId = masterSubPackageId,
            storeTags = storeTags,
            steamDeckMinimumSupport = steamDeckMinimumSupport,
            sortBy = sortBy,
            sortByDirection = sortByDirection
        )
    }
}