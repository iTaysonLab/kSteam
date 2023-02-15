package bruhcollective.itaysonlab.ksteam.models.pics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageInfo(
    @SerialName("packageid") val packageId: Int,
    @SerialName("billingtype") val billingType: Int,
    @SerialName("licensetype") val licenseType: Int,
    val status: Int,
    val extended: PackageInfoExtended = PackageInfoExtended(),
    @SerialName("appids") val appIds: List<Int> = emptyList(),
    @SerialName("depotids") val depotIds: List<Int> = emptyList()
) {
    @Serializable
    data class PackageInfoExtended(
        @SerialName("allowcrossregiontradingandgifting") val allowInternationalGifting: Boolean = false
    )
}