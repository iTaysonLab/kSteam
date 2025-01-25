package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.*
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.*
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo

@Dao
internal interface RoomPicsApplicationDao {
    @Upsert
    suspend fun upsertAppInfo(
        info: RoomPicsAppInfo,
        vdf: RoomPicsAppInfoVdf,
        categories: List<RoomPicsAppInfoCategory>,
        localizedAssets: List<RoomPicsAppInfoLocalizedAssets>,
        storeTagJunction: List<RoomPicsAppInfoStoreTagJunction>,
    )

    @Query("SELECT * FROM app_info WHERE id = :id")
    @Transaction
    suspend fun getFullApplicationById(id: Int): RoomPicsFullAppInfo?

    @Query("SELECT * FROM app_info WHERE id IN (:ids)")
    @Transaction
    suspend fun getFullApplicationByIds(ids: List<Int>): List<RoomPicsFullAppInfo>

    @RawQuery(observedEntities = [RoomPicsAppInfo::class])
    suspend fun rawFilteredApplications(query: RoomRawQuery): List<RoomPicsFullAppInfo>

    @Transaction
    suspend fun upsertAppInfo(
        appInfo: AppInfo,
        compressedData: ByteArray,
    ) {
        upsertAppInfo(
            info = RoomPicsAppInfo(appInfo),
            vdf = RoomPicsAppInfoVdf(
                appid = appInfo.appId,
                data = compressedData,
            ),
            categories = appInfo.common?.category?.keys?.mapNotNull {
                it.removePrefix("category_").toIntOrNull()?.let { cid -> RoomPicsAppInfoCategory(appId = appInfo.appId, categoryId = cid) }
            }.orEmpty(),
            localizedAssets = SteamApplication.extractLocalizedAssetsFrom(appInfo).entries.map { (lang, pack) ->
                RoomPicsAppInfoLocalizedAssets(appInfo.appId, lang, pack)
            },
            storeTagJunction = appInfo.common?.tags?.map {
                RoomPicsAppInfoStoreTagJunction(appId = appInfo.appId, tagId = it)
            }.orEmpty()
        )
    }
}