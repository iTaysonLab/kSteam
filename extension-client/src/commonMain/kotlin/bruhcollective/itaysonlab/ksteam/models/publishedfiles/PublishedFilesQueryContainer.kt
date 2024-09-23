package bruhcollective.itaysonlab.ksteam.models.publishedfiles

data class PublishedFilesQueryContainer (
    val files: List<PublishedFile>,
    val total: Int,
    val nextCursor: String?
)