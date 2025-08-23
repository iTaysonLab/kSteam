package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EPublishedFileInfoMatchingFileType
import bruhcollective.itaysonlab.ksteam.models.enums.EPublishedFileQueryType
import bruhcollective.itaysonlab.ksteam.models.enums.EWorkshopFileType
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFile
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFileApplication
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFilesContainer
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFilesQueryContainer
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobRemoteException
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import okio.IOException
import steam.webui.publishedfile.CPublishedFile_GetDetails_Request
import steam.webui.publishedfile.CPublishedFile_GetUserFiles_Request
import steam.webui.publishedfile.CPublishedFile_QueryFiles_Request
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
     *
     * @param appId [AppId] of a requested app
     * @param fileType file type that should be returned
     * @param count how many items to return, limit is 1000
     * @param queryType search cursor, default is '*' and represents first page
     * @param cursor search cursor, default is '*' and represents first page
     * @param
     */
    @Throws(CMJobRemoteException::class, IOException::class, CancellationException::class)
    suspend fun queryFiles(
        appId: AppId,
        fileType: EPublishedFileInfoMatchingFileType,
        count: Int,
        queryType: EPublishedFileQueryType = EPublishedFileQueryType.RankedByTrend,
        cursor: String = "*",
        searchText: String? = null,
    ): PublishedFilesQueryContainer {
        val response = steamClient.grpc.publishedFile.QueryFiles().executeSteam(
            data = CPublishedFile_QueryFiles_Request(
                appid = appId.value,
                query_type = queryType.ordinal,
                cursor = cursor,
                filetype = EPublishedFileInfoMatchingFileType.WebGuides.ordinal,
                return_kv_tags = true,
                return_details = true,
                return_metadata = true,
                return_previews = true,
                return_reactions = true,
                return_short_description = true,
                return_vote_data = true,
                numperpage = 20
            )
        )

        return TODO()
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
     * @param returnApps return application information
     *
     * @return the object with total list of apps, published files related to them and total amount of queried files
     */
    @Throws(CMJobRemoteException::class, IOException::class, CancellationException::class)
    suspend fun getFiles(
        steamId: SteamId,
        fileType: EPublishedFileInfoMatchingFileType,
        count: Int,
        page: Int = 1,
        appId: AppId? = null,
        sortOrder: PersonalSortOrder = PersonalSortOrder.LastUpdated,
        privacyFilter: PersonalPrivacyFilter = PersonalPrivacyFilter.Everything,
        browseFilter: PersonalBrowseFilter = PersonalBrowseFilter.Created,
        returnApps: Boolean = true,
    ): PublishedFilesContainer {
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

        val response = steamClient.grpc.publishedFile.GetUserFiles().executeSteam(
            data = CPublishedFile_GetUserFiles_Request(
                steamid = steamId.longId,
                appid = appId?.value ?: 0,
                privacy = privacyBit,
                filetype = fileType.ordinal,
                page = page,
                numperpage = count,
                type = type,
                sortmethod = sort,
                return_kv_tags = true,
                return_metadata = true,
                return_previews = true,
                return_apps = returnApps,
                return_tags = true,
                return_reactions = true
            )
        )

        return PublishedFilesContainer(
            apps = response.apps.mapNotNull { app ->
                PublishedFileApplication(id = AppId(app.appid ?: return@mapNotNull null), name = app.name ?: return@mapNotNull null)
            },
            files = response.publishedfiledetails.map(::mapProtoToKs),
            total = response.total ?: 0,
            startIndex = response.startindex ?: 0
        )
    }

    /**
     * Returns information about published files.
     *
     * @param appId application ID of the files
     * @param fileIds ID of files that should be resolved
     *
     * @return a list of resolved files
     */
    @Throws(CMJobRemoteException::class, IOException::class, CancellationException::class)
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
            favorites = file.favorited ?: 0,
            votesUp = file.vote_data?.votes_up ?: 0,
            votesDown = file.vote_data?.votes_down ?: 0,
            voteScore = file.vote_data?.score ?: 0f,
            comments = file.num_comments_public ?: 0,
            views = file.views ?: 0,
            fileSize = file.file_size ?: 0L,
            previewFileSize = file.preview_file_size ?: 0L,
            markedAsSpoiler = file.spoiler_tag == true,
            canBeDeleted = file.can_be_deleted == true,
            kvTags = file.kvtags.associate { it.key.orEmpty() to it.value_.orEmpty() },
            tags = file.tags.map { PublishedFile.SharedInformation.WorkshopTag(tag = it.tag.orEmpty(), displayName = it.display_name.orEmpty()) },
            description = file.file_description ?: file.short_description.orEmpty(),
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