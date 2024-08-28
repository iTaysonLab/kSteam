package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import bruhcollective.itaysonlab.ksteam.util.executeSteamOrNull
import steam.webui.common.*
import steam.webui.community.CCommunity_GetAppRichPresenceLocalization_Request

/**
 * Access store data using this interface.
 */
class Store internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    // TODO: make it "compressable" or store in some kind of LRU cache with on-disk
    private val storeItemsMap = mutableMapOf<StoreItemID, StoreItem>()

    /**
     * Get the latest rich presence localization strings.
     *
     * @param appId ID of an application
     * @param language desired RP language
     *
     * @return rich presence localization strings, or null in case nothing was found
     */
    suspend fun getRichPresenceLocalization(
        appId: Int,
        language: ELanguage = steamClient.language
    ): Map<String, String>? {
        return steamClient.grpc.community.GetAppRichPresenceLocalization().executeSteamOrNull(
            data = CCommunity_GetAppRichPresenceLocalization_Request(
                appid = appId,
                language = language.vdfName
            )
        )?.token_lists?.firstOrNull()?.tokens?.associate { it.name.orEmpty() to it.value_.orEmpty() }
    }

    suspend fun getAppSummaries(appIds: List<Int>): Map<Int, AppSummary> {
        if (appIds.isEmpty()) return emptyMap()

        val picsSummaries = emptyMap<Int, AppSummary>() // steamClient.getImplementingHandlerOrNull<MetadataPlugin>()?.getMetadataFor(appIds) ?: emptyMap()

        val netSummaries = if (picsSummaries.isNotEmpty()) {
            getNetworkApps(appIds.filterNot { picsSummaries.containsKey(it) })
        } else {
            getNetworkApps(appIds)
        }

        return netSummaries + picsSummaries
    }

    /**
     * Gets app summaries from the Steam Store.
     */
    suspend fun getNetworkApps(ids: List<Int>): Map<Int, AppSummary> {
        return getStoreItemsTyped(
            ids = ids.map { StoreItemID(appid = it) },
            transformer = ::AppSummary
        ).mapKeys {
            it.key.appid ?: error("Store/GetAppSummaries: mapKeys key is null appid?")
        }
    }

    suspend fun getNetworkApp(appId: Int): AppSummary = getNetworkApps(listOf(appId)).values.first()

    suspend fun getNetworkPackages(ids: List<Int>): Map<Int, AppSummary> {
        return getStoreItemsTyped(
            ids = ids.map { StoreItemID(appid = it) },
            transformer = ::AppSummary
        ).mapKeys {
            it.key.appid ?: error("Store/GetAppSummaries: mapKeys key is null appid?")
        }
    }

    suspend fun getNetworkPackage(appId: Int): AppSummary = getNetworkApps(listOf(appId)).values.first()

    /**
     * Gets details from Store.
     *
     * This request will be cached in a temporary "DB" which will be reset at kSteam relaunch.
     */
    suspend fun <T> getStoreItemsTyped(ids: List<StoreItemID>, transformer: (StoreItem) -> T): Map<StoreItemID, T> {
        return getStoreItems(ids).mapValues { transformer(it.value) }
    }

    suspend fun getStoreItems(ids: List<StoreItemID>): Map<StoreItemID, StoreItem> {
        if (ids.isEmpty()) {
            return emptyMap() // short-circuit
        }

        browseOnlineStoreForIds(ids.filterNot(storeItemsMap::containsKey)).forEach { onlineStoreItem ->
            storeItemsMap[storeItemToId(onlineStoreItem)] = onlineStoreItem
        }

        return ids.asSequence().filter(storeItemsMap::containsKey).associateWith(storeItemsMap::getValue)
    }

    private suspend fun browseOnlineStoreForIds(
        ids: List<StoreItemID>,
        request: StoreBrowseItemDataRequest = DefaultStoreBrowseItemDataRequest
    ): List<StoreItem> {
        if (ids.isEmpty()) {
            return emptyList() // short-circuit
        }

        return steamClient.grpc.storeBrowse.GetItems().executeSteam(data = CStoreBrowse_GetItems_Request(
            ids = ids,
            context = StoreBrowseContext(
                language = steamClient.language.vdfName,
                country_code = steamClient.persona.currentPersona.value.country
            ), data_request = request
        )).store_items
    }

    private fun storeItemToId(item: StoreItem) = when (item.item_type) {
        0 -> StoreItemID(appid = item.appid)
        1 -> StoreItemID(packageid = item.id)
        2 -> StoreItemID(bundleid = item.id)
        4 -> StoreItemID(tagid = item.id)
        5 -> StoreItemID(creatorid = item.id)
        6 -> StoreItemID(hubcategoryid = item.id)
        else -> error("[storeItemToId] unsupported type ${item.item_type} for proto item ${item}")
    }

    private companion object {
        // Include everything
        val DefaultStoreBrowseItemDataRequest = StoreBrowseItemDataRequest(
            include_assets = true,
            include_release = true,
            include_platforms = true,
            include_all_purchase_options = true,
            include_screenshots = true,
            include_trailers = true,
            include_ratings = true,
            include_tag_count = 5,
            include_reviews = true,
            include_basic_info = true,
            include_supported_languages = true
        )
    }
}