package bruhcollective.itaysonlab.ksteam.models.news

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class NewsJsonData (
    @SerialName("localized_subtitle")
    val subtitles: List<String?> = emptyList(),
    @SerialName("localized_summary")
    val summaries: List<String?> = emptyList(),
    @SerialName("localized_title_image")
    val titleImages: List<String?> = emptyList(),
    @SerialName("localized_capsule_image")
    val capsuleImages: List<String?> = emptyList(),
)