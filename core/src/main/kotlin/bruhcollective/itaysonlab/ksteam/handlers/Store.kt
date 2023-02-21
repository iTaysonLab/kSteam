package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import kotlinx.serialization.json.Json
import steam.webui.common.*
import steam.webui.community.CCommunity_GetAppRichPresenceLocalization_Request
import steam.webui.community.CCommunity_GetAppRichPresenceLocalization_Response

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
    private val appSummaryDetailMap = mutableMapOf<AppId, AppSummary>()
    private val rpLocalizationMap = mutableMapOf<AppId, Map<String, String>>()

    suspend fun getRichPresenceLocalization(appId: AppId): Map<String, String> {
        if (rpLocalizationMap.containsKey(appId)) {
            return rpLocalizationMap[appId] ?: emptyMap()
        }

        return (steamClient.webApi.execute(
            methodName = "Community.GetAppRichPresenceLocalization",
            requestAdapter = CCommunity_GetAppRichPresenceLocalization_Request.ADAPTER,
            responseAdapter = CCommunity_GetAppRichPresenceLocalization_Response.ADAPTER,
            requestData = CCommunity_GetAppRichPresenceLocalization_Request(
                appid = appId.id, language = steamClient.config.language.vdfName
            )
        ).dataNullable?.token_lists?.firstOrNull()?.tokens?.associate { it.name.orEmpty() to it.value_.orEmpty() } ?: emptyMap()).also {
            rpLocalizationMap[appId] = it
        }
    }

    suspend fun getAppSummaries(appId: List<AppId>): Map<AppId, AppSummary> {
        val picsSummaries = steamClient.pics.getAppSummariesByAppId(appId)
        val netSummaries = getApps(appId.filterNot { picsSummaries.containsKey(it) })

        return picsSummaries + netSummaries
    }

    /**
     * Gets app details from Store.
     *
     * This request will be cached in a temporary "DB" which will be reset at kSteam relaunch.
     */
    suspend fun getApps(appIds: List<AppId>): Map<AppId, AppSummary> {
        if (appIds.isEmpty()) return emptyMap()

        val appIdsParts = appIds.partition {
            appSummaryDetailMap.containsKey(it)
        }

        val networkAppSummaries = if (appIdsParts.second.isNotEmpty()) {
            steamClient.webApi.execute(
                methodName = "StoreBrowse.GetItems",
                requestAdapter = CStoreBrowse_GetItems_Request.ADAPTER,
                responseAdapter = CStoreBrowse_GetItems_Response.ADAPTER,
                requestData = CStoreBrowse_GetItems_Request(
                    ids = appIdsParts.second.map { StoreItemID(appid = it.id) },
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
                AppSummary(it)
            }.also { appSummaryDetailMap.putAll(it.associateBy { p -> p.id }) }
        } else {
            emptyList()
        }

        return (appIdsParts.first.map { appSummaryDetailMap[it]!! } + networkAppSummaries).associateBy {
            it.id
        }
    }

    suspend fun getApp(appId: AppId): AppSummary = getApps(listOf(appId)).values.first()
}