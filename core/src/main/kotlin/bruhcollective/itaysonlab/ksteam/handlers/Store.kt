package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.apps.App
import kotlinx.serialization.json.Json
import steam.webui.common.*

/**
 * Access store data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Store internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    private val json = Json { ignoreUnknownKeys = true }

    // TODO: make it "compressable" or store in some kind of LRU cache with on-disk
    private val appDetailMap = mutableMapOf<AppId, App>()

    /**
     * Gets app details from Store.
     *
     * This request will be cached in a temporary "DB" which will be reset at kSteam relaunch.
     */
    suspend fun getApps(appIds: List<AppId>): Map<AppId, App> {
        val appIdsParts = appIds.partition {
            appDetailMap.containsKey(it)
        }

        val networkApps = if (appIdsParts.second.isNotEmpty()) {
            steamClient.webApi.execute(
                methodName = "StoreBrowse.GetItems",
                requestAdapter = CStoreBrowse_GetItems_Request.ADAPTER,
                responseAdapter = CStoreBrowse_GetItems_Response.ADAPTER,
                requestData = CStoreBrowse_GetItems_Request(
                    ids = appIdsParts.second.map { StoreItemID(appid = it.id.toInt()) },
                    context = StoreBrowseContext(
                        language = steamClient.config.language.vdfName,
                        country_code = steamClient.persona.currentPersona.value.country
                    ), data_request = StoreBrowseItemDataRequest(
                        include_assets = true,
                        include_release = true,
                        include_platforms = true,
                        include_all_purchase_options = true,
                        include_screenshots = false,
                        include_trailers = false,
                        include_ratings = false,
                        include_tag_count = 5,
                        include_reviews = true,
                        include_basic_info = true,
                        include_supported_languages = true
                    )
                )
            ).data.store_items.map {
                App(it)
            }.also { appDetailMap.putAll(it.associateBy { p -> p.id }) }
        } else {
            emptyList()
        }

        return (appIdsParts.first.map { appDetailMap[it]!! } + networkApps).associateBy {
            it.id
        }
    }

    suspend fun getApp(appId: AppId): App = getApps(listOf(appId)).values.first()
}