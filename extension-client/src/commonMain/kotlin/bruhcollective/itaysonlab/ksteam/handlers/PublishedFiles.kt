package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFile
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import steam.webui.publishedfile.CPublishedFile_GetDetails_Request

/**
 * Access Steam User-Generated content using this handler.
 */
class PublishedFiles internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    private companion object {
        private const val LOG_TAG = "CoreExt:PublishedFiles"
    }

    /**
     * Returns information about published files
     */
    suspend fun getDetails(
        appId: Int,
        fileIds: List<Long>
    ): List<PublishedFile> {
        return steamClient.grpc.publishedFile.GetDetails().executeSteam(
            data = CPublishedFile_GetDetails_Request(
                appid = appId,
                publishedfileids = fileIds
            )
        ).publishedfiledetails.map { file ->
            val creatorSteamId = file.creator.toSteamId()
            val creatorPersona = steamClient.persona.persona(creatorSteamId)

            when (file.file_type ?: 0) {
                5 -> {
                    PublishedFile.Screenshot(
                        id = file.publishedfileid ?: 0,
                        creatorSteamId = creatorSteamId,
                        creatorPersona = creatorPersona,
                        creationDate = file.time_created ?: 0,
                        lastUpdateDate = file.time_updated ?: 0,
                        likes = file.favorited ?: 0,
                        comments = file.num_comments_public ?: 0,
                        views = file.views ?: 0,
                        //
                        fullImageUrl = file.image_url.orEmpty(),
                        previewImageUrl = file.preview_url.orEmpty(),
                        imageHeight = file.image_height ?: 0,
                        imageWidth = file.image_width ?: 0,
                        isSpoiler = file.spoiler_tag ?: false
                    )
                }

                else -> {
                    PublishedFile.Unknown(
                        id = file.publishedfileid ?: 0,
                        creatorSteamId = creatorSteamId,
                        creatorPersona = creatorPersona,
                        creationDate = file.time_created ?: 0,
                        lastUpdateDate = file.time_updated ?: 0,
                        likes = file.favorited ?: 0,
                        comments = file.num_comments_public ?: 0,
                        views = file.views ?: 0,
                        proto = file
                    )
                }
            }
        }
    }
}