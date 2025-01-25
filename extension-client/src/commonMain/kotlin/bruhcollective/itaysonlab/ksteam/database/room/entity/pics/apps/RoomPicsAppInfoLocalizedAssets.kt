package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.*
import bruhcollective.itaysonlab.ksteam.database.room.TableNames
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage

@Entity(
    tableName = TableNames.APP_INFO_LOCALE_NAMES,
    primaryKeys = ["app_id", "lang"],
    foreignKeys = [
        ForeignKey(
            entity = RoomPicsAppInfo::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("name")]
)
internal data class RoomPicsAppInfoLocalizedAssets(
    @ColumnInfo("app_id")
    val appId: Int,

    @ColumnInfo(name = "lang")
    val language: String,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "small_capsule")
    val smallCapsule: String?,

    @Embedded(prefix = "header_image_")
    val headerImage: String?,

    @Embedded(prefix = "library_hero_")
    val libraryHero: RetinaAsset?,

    @Embedded(prefix = "library_hero_blur_")
    val libraryHeroBlur: RetinaAsset?,

    @Embedded(prefix = "library_logo_")
    val libraryLogo: RetinaAsset?,

    @Embedded(prefix = "library_capsule_")
    val libraryCapsule: RetinaAsset?,

    @Embedded(prefix = "library_header_")
    val libraryHeader: RetinaAsset?,
) {
    data class RetinaAsset (
        @ColumnInfo(name = "path")
        val path: String,

        @ColumnInfo(name = "path_2x")
        val path2x: String?,
    )

    constructor(appId: Int, lang: ELanguage, pack: SteamApplication.Assets.LocalizedAssetPack): this(
        appId = appId,
        language = lang.vdfName,
        name = pack.name,
        smallCapsule = pack.smallCapsule,
        headerImage = pack.headerImage,
        libraryHero = pack.libraryHero?.let { RetinaAsset(path = it.path, path2x = it.path2x) },
        libraryHeader = pack.libraryHeader?.let { RetinaAsset(path = it.path, path2x = it.path2x) },
        libraryCapsule = pack.libraryCapsule?.let { RetinaAsset(path = it.path, path2x = it.path2x) },
        libraryLogo = pack.libraryLogo?.let { RetinaAsset(path = it.path, path2x = it.path2x) },
        libraryHeroBlur = pack.libraryHeroBlur?.let { RetinaAsset(path = it.path, path2x = it.path2x) },
    )
}