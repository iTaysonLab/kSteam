package bruhcollective.itaysonlab.ksteam.models.publishedfiles

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import kotlinx.coroutines.flow.Flow
import steam.webui.publishedfile.PublishedFileDetails

@Immutable
sealed interface PublishedFile {
    val id: Long

    val creatorSteamId: SteamId
    val creatorPersona: Flow<Persona>

    val views: Int
    val likes: Int
    val comments: Int

    val creationDate: Int
    val lastUpdateDate: Int

    data class Screenshot(
        override val id: Long,
        override val creatorSteamId: SteamId,
        override val creatorPersona: Flow<Persona>,
        override val creationDate: Int,
        override val lastUpdateDate: Int,
        override val likes: Int,
        override val comments: Int,
        override val views: Int,
        //
        val fullImageUrl: String,
        val previewImageUrl: String,
        val imageHeight: Int,
        val imageWidth: Int,
        val isSpoiler: Boolean
    ): PublishedFile

    data class Unknown(
        override val id: Long,
        override val creatorSteamId: SteamId,
        override val creatorPersona: Flow<Persona>,
        override val creationDate: Int,
        override val lastUpdateDate: Int,
        override val likes: Int,
        override val comments: Int,
        override val views: Int,
        val proto: PublishedFileDetails
    ): PublishedFile
}