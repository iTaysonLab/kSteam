package bruhcollective.itaysonlab.ksteam.models.library

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
data class LibraryShelf (
    val id: String,
    val linkedCollection: String,
    val sortBy: Int,
    val lastChangedMs: Long,
    val orderTimestamp: Long,
    val version: Long,
    val remoteTimestamp: Int,
) {
    @Serializable
    data class LibraryShelfRemote (
        @SerialName("strCollectionId") val linkedCollection: String,
        @SerialName("eSortBy") val sortBy: Int = 1,
        @SerialName("nLastChangedMS") val lastChangedMs: Long = 0L,
        @SerialName("nOrder") val orderTimestamp: Long = 0L,
    )
}