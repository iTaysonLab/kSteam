package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.*
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsAppEntry
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.*
import bruhcollective.itaysonlab.ksteam.database.room.entity.store.RoomStoreTag
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo

@Dao
internal interface RoomPicsApplicationDao {
    @Upsert
    suspend fun upsertAppInfo(
        entries: List<RoomPicsAppEntry>,
        info: List<RoomPicsAppInfo>,
        categories: List<RoomPicsAppInfoCategory>,
        localizedAssets: List<RoomPicsAppInfoLocalizedAssets>,
        storeTagJunction: List<RoomPicsAppInfoStoreTagJunction>,
        associations: List<RoomPicsAppInfoAssociation>,
        contentDescriptor: List<RoomPicsAppInfoContentDescriptor>,
        storeTags: List<RoomStoreTag>
    )

    // Full version

    @Query("SELECT * FROM app_info WHERE id = :id")
    @Transaction
    suspend fun getFullApplicationById(id: Int): RoomPicsFullAppInfo?

    @Query("SELECT * FROM app_info WHERE id IN (:ids)")
    @Transaction
    suspend fun getFullApplicationByIds(ids: List<Int>): List<RoomPicsFullAppInfo>

    @RawQuery(observedEntities = [RoomPicsAppInfo::class])
    suspend fun rawFilteredApplicationsFull(query: RoomRawQuery): List<RoomPicsFullAppInfo>

    // Lite version

    @Query("SELECT * FROM app_info WHERE id = :id")
    @Transaction
    suspend fun getApplicationById(id: Int): RoomPicsAppInfo?

    @Query("SELECT * FROM app_info WHERE id IN (:ids)")
    @Transaction
    suspend fun getApplicationByIds(ids: List<Int>): List<RoomPicsAppInfo>

    @RawQuery(observedEntities = [RoomPicsAppInfo::class])
    suspend fun rawFilteredApplications(query: RoomRawQuery): List<RoomPicsAppInfo>

    @Transaction
    suspend fun upsertAppInfo(entry: PendingApplicationEntry) {
        upsertAppInfo(
            entries = entry.entries,
            info = entry.info,
            categories = entry.categories,
            localizedAssets = entry.localizedAssets,
            storeTagJunction = entry.storeTagJunction,
            associations = entry.associations,
            contentDescriptor = entry.contentDescriptor,
            storeTags = entry.storeTags.distinctBy(RoomStoreTag::id),
        )
    }

    class PendingApplicationEntry {
        val entries: MutableList<RoomPicsAppEntry> = mutableListOf()
        val info: MutableList<RoomPicsAppInfo> = mutableListOf()
        val categories: MutableList<RoomPicsAppInfoCategory> = mutableListOf()
        val localizedAssets: MutableList<RoomPicsAppInfoLocalizedAssets> = mutableListOf()
        val storeTagJunction: MutableList<RoomPicsAppInfoStoreTagJunction> = mutableListOf()
        val associations: MutableList<RoomPicsAppInfoAssociation> = mutableListOf()
        val contentDescriptor: MutableList<RoomPicsAppInfoContentDescriptor> = mutableListOf()
        val storeTags: MutableList<RoomStoreTag> = mutableListOf()

        operator fun plusAssign(entry: RoomPicsAppEntry) {
            entries.add(entry)
        }

        operator fun plusAssign(appInfo: AppInfo) {
            info.add(RoomPicsAppInfo(appInfo))

            categories += appInfo.common?.category?.keys?.mapNotNull {
                it.removePrefix("category_").toIntOrNull()?.let { cid -> RoomPicsAppInfoCategory(appId = appInfo.appId, categoryId = cid) }
            }.orEmpty()

            localizedAssets += SteamApplication.extractLocalizedAssetsFrom(appInfo, false).entries.map { (lang, pack) ->
                RoomPicsAppInfoLocalizedAssets(appInfo.appId, lang, pack)
            }

            storeTagJunction += appInfo.common?.tags?.map {
                RoomPicsAppInfoStoreTagJunction(appId = appInfo.appId, tagId = it)
            }.orEmpty()

            associations += appInfo.common?.associations?.map { assoc ->
                RoomPicsAppInfoAssociation(appId = appInfo.appId, type = assoc.type, name = assoc.name)
            }.orEmpty()

            contentDescriptor += appInfo.common?.contentDescriptors?.map { desc ->
                RoomPicsAppInfoContentDescriptor(appId = appInfo.appId, descriptor = desc.toInt())
            }.orEmpty()

            storeTags += appInfo.common?.tags?.map {
                RoomStoreTag(id = it, language = "english", name = "", normalizedName = "")
            }.orEmpty()
        }
    }
}