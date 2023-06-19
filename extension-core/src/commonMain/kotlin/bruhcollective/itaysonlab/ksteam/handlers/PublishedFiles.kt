package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import steam.webui.publishedfile.CPublishedFile_GetDetails_Request
import steam.webui.publishedfile.CPublishedFile_GetDetails_Response

/**
 * Access Steam User-Generated content using this handler.
 */
class PublishedFiles internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    private companion object {
        private const val LOG_TAG = "CoreExt:PublishedFiles"
    }

    /**
     * Returns information about published files
     */
    suspend fun getDetails(
        appId: Int,
        fileIds: List<Long>
    ) {
        steamClient.unifiedMessages.execute(
            methodName = "PublishedFile.GetDetails",
            requestAdapter = CPublishedFile_GetDetails_Request.ADAPTER,
            responseAdapter = CPublishedFile_GetDetails_Response.ADAPTER,
            requestData = CPublishedFile_GetDetails_Request(
                appid = appId,
                publishedfileids = fileIds
            )
        ).let(::println)
    }
}