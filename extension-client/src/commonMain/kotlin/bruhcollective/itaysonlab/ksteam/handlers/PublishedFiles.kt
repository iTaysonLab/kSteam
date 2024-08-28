package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EPublishedFileInfoMatchingFileType
import bruhcollective.itaysonlab.ksteam.models.enums.EWorkshopFileType
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFile
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import bruhcollective.itaysonlab.ksteam.util.SteamRpcException
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import okio.IOException
import steam.webui.publishedfile.CPublishedFile_GetDetails_Request
import steam.webui.publishedfile.CPublishedFile_GetUserFiles_Request
import steam.webui.publishedfile.PublishedFileDetails
import kotlin.coroutines.cancellation.CancellationException

/**
 * Access Steam User-Generated content using this handler.
 */
class PublishedFiles internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    /**
     * Queries all public files of a specific [AppId].
     */
    @Throws(SteamRpcException::class, IOException::class, CancellationException::class)
    suspend fun queryFiles() {
        TODO()
    }

    /**
     * Queries the list of apps for which [SteamId] have files.
     *
     * @param steamId Steam ID of a person
     * @param fileType type of files to filter
     * @param browseFilter browsing filter - created or added to favorites
     *
     * @return the list of apps
     */
    @Throws(SteamRpcException::class, IOException::class, CancellationException::class)
    suspend fun getFilesAppList(
        steamId: SteamId,
        fileType: EPublishedFileInfoMatchingFileType,
        browseFilter: PersonalBrowseFilter = PersonalBrowseFilter.Created
    ): List<PublishedFileApplication> {
        val type = when (browseFilter) {
            PersonalBrowseFilter.Created -> "myfiles"
            PersonalBrowseFilter.Favorites -> "myfavorites"
        }

        return steamClient.grpc.publishedFile.GetUserFiles().executeSteam(
            data = CPublishedFile_GetUserFiles_Request(
                steamid = steamId.longId,
                appid = 0,
                filetype = fileType.ordinal,
                numperpage = 1,
                type = type,
                sortmethod = "lastupdated",
                return_kv_tags = false,
                return_metadata = false,
                return_previews = false,
                return_apps = true,
                return_short_description = false,
                return_tags = false,
                return_reactions = false
            )
        ).apps.mapNotNull { app ->
            PublishedFileApplication(id = AppId(app.appid ?: return@mapNotNull null), name = app.name ?: return@mapNotNull null)
        }
    }

    /**
     * Queries files of a specified [SteamId].
     *
     * @param steamId Steam ID of a person
     * @param fileType type of files to return
     * @param count count of files per page
     * @param page page index, starts from 1
     * @param appId filter by app ID, if present
     * @param sortOrder sorting order
     * @param privacyFilter file visibility filter
     * @param browseFilter browsing filter - created or added to favorites
     *
     * @return the list of files
     */
    @Throws(SteamRpcException::class, IOException::class, CancellationException::class)
    suspend fun getFiles(
        steamId: SteamId,
        fileType: EPublishedFileInfoMatchingFileType,
        count: Int,
        page: Int = 1,
        appId: AppId? = null,
        sortOrder: PersonalSortOrder = PersonalSortOrder.LastUpdated,
        privacyFilter: PersonalPrivacyFilter = PersonalPrivacyFilter.Everything,
        browseFilter: PersonalBrowseFilter = PersonalBrowseFilter.Created,
    ): List<PublishedFile> {
        val sort = when (sortOrder) {
            PersonalSortOrder.MostPopular -> "score"
            PersonalSortOrder.NewestFirst -> "newestfirst"
            PersonalSortOrder.OldestFirst -> "oldestfirst"
            PersonalSortOrder.LastUpdated -> "lastupdated"
        }

        val privacyBit = when (privacyFilter) {
            PersonalPrivacyFilter.Everything -> 30
            PersonalPrivacyFilter.LinkOnly -> 16
            PersonalPrivacyFilter.Public -> 8
            PersonalPrivacyFilter.FriendsOnly -> 4
            PersonalPrivacyFilter.Private -> 2
        }

        val type = when (browseFilter) {
            PersonalBrowseFilter.Created -> "myfiles"
            PersonalBrowseFilter.Favorites -> "myfavorites"
        }

        return steamClient.grpc.publishedFile.GetUserFiles().executeSteam(
            data = CPublishedFile_GetUserFiles_Request(
                steamid = steamId.longId,
                appid = appId?.value ?: 0,
                privacy = privacyBit,
                filetype = fileType.ordinal,
                numperpage = count,
                type = type,
                sortmethod = sort,
                return_kv_tags = true,
                return_metadata = true,
                return_previews = true,
                return_apps = false,
                return_short_description = true,
                return_tags = true,
                return_reactions = true
            )
        ).publishedfiledetails.map(::mapProtoToKs)
    }

    /**
     * Returns information about published files.
     *
     * @param appId application ID of the files
     * @param fileIds ID of files that should be resolved
     *
     * @return a list of resolved files
     */
    @Throws(SteamRpcException::class, IOException::class, CancellationException::class)
    suspend fun getDetails(
        appId: Int,
        fileIds: List<Long>
    ): List<PublishedFile> {
        return steamClient.grpc.publishedFile.GetDetails().executeSteam(
            data = CPublishedFile_GetDetails_Request(
                appid = appId,
                publishedfileids = fileIds
            )
        ).publishedfiledetails.map(::mapProtoToKs)
    }

    private fun mapProtoToKs(file: PublishedFileDetails): PublishedFile {
        val id = file.publishedfileid ?: 0

        val sharedInformation = PublishedFile.SharedInformation(
            creatorSteamId = file.creator.toSteamId(),
            creationDate = file.time_created ?: 0,
            lastUpdateDate = file.time_updated ?: 0,
            url = file.url.orEmpty(),
            previewUrl = file.preview_url.orEmpty(),
            fileUrl = file.file_url.orEmpty(),
            fileName = file.filename.orEmpty(),
            appId = AppId(file.consumer_appid ?: 0),
            appName = file.app_name.orEmpty(),
            likes = file.favorited ?: 0,
            comments = file.num_comments_public ?: 0,
            views = file.views ?: 0,
            fileSize = file.file_size ?: 0L,
            previewFileSize = file.preview_file_size ?: 0L,
            markedAsSpoiler = file.spoiler_tag == true,
            canBeDeleted = file.can_be_deleted == true,
            tags = file.kvtags.associate { it.key.orEmpty() to it.value_.orEmpty() },
            reactions = file.reactions.mapNotNull { reaction ->
                PublishedFile.SharedInformation.ReactionCount(
                    id = reaction.reactionid ?: return@mapNotNull null,
                    count = reaction.count ?: return@mapNotNull null
                )
            }
        )

        return when (file.file_type ?: 0) {
            EWorkshopFileType.Screenshot.ordinal -> {
                PublishedFile.Screenshot(
                    id = id,
                    info = sharedInformation,
                    imageHeight = file.image_height ?: 0,
                    imageWidth = file.image_width ?: 0,
                    imageUrl = file.image_url.orEmpty()
                )
            }

            else -> {
                PublishedFile.Unknown(
                    id = id,
                    info = sharedInformation,
                    proto = file
                )
            }
        }
    }

    data class PublishedFileApplication(
        val id: AppId,
        val name: String
    )

    enum class PersonalSortOrder {
        MostPopular,
        NewestFirst,
        OldestFirst,
        LastUpdated
    }

    enum class PersonalPrivacyFilter {
        Everything, // 30
        Public, // 8
        FriendsOnly, // 4
        Private, // 2
        LinkOnly, // 16
    }

    enum class PersonalBrowseFilter {
        Created,
        Favorites
    }
}