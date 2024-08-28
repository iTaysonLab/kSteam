package bruhcollective.itaysonlab.ksteam.models.publishedfiles

import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import steam.webui.publishedfile.PublishedFileDetails

@Immutable
sealed interface PublishedFile {
    /**
     * Unique ID of the file.
     */
    val id: Long

    /**
     * Shared information that is common for every file type.
     */
    val info: SharedInformation

    /**
     * Shared information that is common for every file type.
     */
    data class SharedInformation (
        /**
         * "File" web URL. Can be empty.
         */
        val url: String,

        /**
         * File URL.
         */
        val fileUrl: String,

        /**
         * Name of this file.
         */
        val fileName: String,

        /**
         * [fileUrl] size, in bytes.
         */
        val fileSize: Long,

        /**
         * [previewUrl] size, in bytes.
         */
        val previewFileSize: Long,

        /**
         * Preview image URL of this file.
         */
        val previewUrl: String,

        /**
         * Application ID that is related to this file.
         */
        val appId: AppId,

        /**
         * Application name that is related to this file.
         */
        val appName: String,

        /**
         * Steam ID of the file creator.
         */
        val creatorSteamId: SteamId,

        /**
         * The amount of views on the file.
         */
        val views: Int,

        /**
         * The amount of likes (thumbs up icon) on the file.
         */
        val likes: Int,

        /**
         * The total amount of comments posted.
         */
        val comments: Int,

        /**
         * File creation timestamp, in seconds
         */
        val creationDate: Int,

        /**
         * Last file update timestamp, in seconds
         */
        val lastUpdateDate: Int,

        /**
         * Is this file marked as a spoiler?
         */
        val markedAsSpoiler: Boolean,

        /**
         * Can this file be deleted by this user?
         */
        val canBeDeleted: Boolean,

        /**
         * Key-Value tags applied to this file.
         *
         * For example, CS:GO screenshots always have a "location" tag containing the map's name (de_mirage, de_cache)
         */
        val tags: Map<String, String>,

        /**
         * Reactions that were applied to this file.
         */
        val reactions: List<ReactionCount>
    ) {
        data class ReactionCount (
            /**
             * ID of the reaction.
             */
            val id: Int,

            /**
             * Amount of this reaction that the file received
             */
            val count: Int
        )
    }

    /**
     * A screenshot that is posted on Steam.
     */
    data class Screenshot (
        override val id: Long,
        override val info: SharedInformation,
        // Screenshot-related
        val imageHeight: Int,
        val imageWidth: Int,
        val imageUrl: String
    ): PublishedFile

    /**
     * kSteam does not know about this file type. Raw protobuf is included.
     */
    data class Unknown(
        override val id: Long,
        override val info: SharedInformation,
        val proto: PublishedFileDetails
    ): PublishedFile
}