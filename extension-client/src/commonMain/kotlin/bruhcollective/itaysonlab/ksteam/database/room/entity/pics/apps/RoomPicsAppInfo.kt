package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.*
import bruhcollective.itaysonlab.ksteam.database.room.TableNames
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsAppEntry
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo

@Entity(tableName = TableNames.APP_INFO, indices = [Index(name = "AppInfoByName", value = ["name"])], foreignKeys = [
    ForeignKey(
        entity = RoomPicsAppEntry::class,
        parentColumns = ["id"],
        childColumns = ["id"],
        onDelete = ForeignKey.CASCADE
    )
])
internal data class RoomPicsAppInfo(
    @PrimaryKey @ColumnInfo("id") val id: Int,

    // Filtering
    @ColumnInfo("name") val name: String,
    @ColumnInfo("sortas") val sortAs: String,
    @ColumnInfo("type") val type: String,
    @ColumnInfo("master_sub") val masterSubPackageId: Int?,
    @ColumnInfo("deck_compat") val steamDeckCompat: Int,
    @ColumnInfo("controller") val controllerSupport: String,

    // Sorting
    @ColumnInfo("steam_release_date") val steamReleaseDate: Long,
    @ColumnInfo("original_release_date") val originalReleaseDate: Long,
    @ColumnInfo("metacritic_score") val metacriticScore: Int?,
    @ColumnInfo("review_score") val reviewScore: Int,

    // Other
    @ColumnInfo("icon") val iconId: String?,
    @ColumnInfo("logo") val logoId: String?,
    @ColumnInfo("clienticon") val clientIconId: String?,

    @ColumnInfo("dlcforappid") val dlcForAppId: Int?,
    @ColumnInfo("community_visible_stats") val hasStats: Boolean,
    @ColumnInfo("community_hub_visible") val hasContentHub: Boolean,
    @ColumnInfo("releasestate") val releaseState: String,
    @ColumnInfo("metacritic_url") val metacriticUrl: String?,
    @ColumnInfo("review_percentage") val reviewPercentage: Int,
    @ColumnInfo("homepage") val homepage: String?,
    @ColumnInfo("gamemanualurl") val manualUrl: String?,

) {
    constructor(ks: AppInfo) : this(
        id = ks.appId,
        name = ks.common?.name.orEmpty(),
        sortAs = ks.common?.sortAs.orEmpty().ifEmpty { ks.common?.name }.orEmpty(),
        type = ks.common?.type?.lowercase().orEmpty(),
        masterSubPackageId = ks.common?.masterSubPackageId,
        steamDeckCompat = ks.common?.steamDeckCompat?.category ?: ESteamDeckSupport.Unknown.ordinal,
        controllerSupport = ks.common?.controllerSupport ?: "",
        iconId = ks.common?.iconId,
        logoId = ks.common?.logoId,
        clientIconId = ks.common?.clientIconId,
        steamReleaseDate = ks.common?.steamReleaseDate ?: 0,
        originalReleaseDate = ks.common?.releaseDate ?: 0,
        metacriticScore = ks.common?.metacriticScore,
        reviewScore = ks.common?.reviewScore ?: 0,
        //
        dlcForAppId = ks.common?.dlcForAppId,
        hasStats = ks.common?.hasStats == true,
        hasContentHub = ks.common?.hasContentHub == true,
        releaseState = ks.common?.releaseState ?: "",
        metacriticUrl = ks.common?.metacriticUrl,
        reviewPercentage = ks.common?.reviewPercentage ?: 0,
        homepage = ks.extended?.homepage,
        manualUrl = ks.extended?.manualUrl,
    )
}