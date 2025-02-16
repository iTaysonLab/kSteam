package bruhcollective.itaysonlab.ksteam.handlers

import androidx.collection.mutableScatterMapOf
import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplicationFactory
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

    /**
     * Get [SteamApplication] by a list of [AppId], using the cache if possible.
     */
    suspend fun querySteamApplications(ids: List<AppId>): List<SteamApplication> {
        if (ids.isEmpty()) return emptyList()

        if (steamClient.enablePics) {
            val cache = mutableScatterMapOf<AppId, SteamApplication>()

            val (cached, notCached) = ids.partition { id ->
                steamClient.pics.getSteamApplication(full = false, id = id.value)?.also { localApplication ->
                    cache[id] = localApplication
                } != null
            }

            return cached.map { id ->
                cache[id] ?: error("Should not happen!")
            } + getNetworkSteamApplications(notCached)
        } else {
            return getNetworkSteamApplications(ids)
        }
    }

    /**
     * Gets app summaries from the Steam Store.
     */
    suspend fun getNetworkSteamApplications(ids: List<AppId>): List<SteamApplication> {
        return getStoreItemsTyped(
            ids = ids.map { StoreItemID(appid = it.value) },
            transformer = SteamApplicationFactory::fromStoreItem
        )
    }

    /**
     * Gets details from Store.
     *
     * This request will be cached in a temporary "DB" which will be reset at kSteam relaunch.
     */
    suspend fun <T> getStoreItemsTyped(ids: List<StoreItemID>, transformer: (StoreItem) -> T): List<T> {
        return browseOnlineStoreForIds(ids = ids).map(transformer)
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
        else -> error("[storeItemToId] unsupported type ${item.item_type} for proto item $item")
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