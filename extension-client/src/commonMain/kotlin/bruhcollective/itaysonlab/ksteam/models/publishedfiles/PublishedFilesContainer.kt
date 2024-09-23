package bruhcollective.itaysonlab.ksteam.models.publishedfiles

data class PublishedFilesContainer (
    val apps: List<PublishedFileApplication>,
    val files: List<PublishedFile>,
    val total: Int,
    val startIndex: Int
)