package bruhcollective.itaysonlab.ksteam.handlers

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
     * Get [SteamApplication] by a list of [AppId].
     *
     * If the client has PICS enabled - getting information from PICS will be prioritized.
     *
     * The order of returned list is not stable. Consider sorting or turning into a hashmap.
     */
    suspend fun querySteamApplications(ids: List<AppId>): List<SteamApplication> {
        if (ids.isEmpty()) return emptyList()

        if (steamClient.enablePics) {
            val picsApplications = steamClient.pics.getSteamApplications(full = true, ids = ids.map(AppId::value))
            val unloadedIds = ids.subtract(picsApplications.map(SteamApplication::id))

            return picsApplications + getNetworkSteamApplications(unloadedIds.toList())
        } else {
            return getNetworkSteamApplications(ids)
        }
    }

    /**
     * Gets app summaries from the Steam Store. Behaves like [querySteamApplications], but bypasses PICS.
     */
    suspend fun getNetworkSteamApplications(ids: List<AppId>): List<SteamApplication> {
        return browseOnlineStoreForIds(
            ids = ids.map { StoreItemID(appid = it.value) },
        ).map(SteamApplicationFactory::fromStoreItem)
    }

    /**
     * Browses the online Store for specific IDs and returns a list of raw StoreItems.
     */
    suspend fun browseOnlineStoreForIds(
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