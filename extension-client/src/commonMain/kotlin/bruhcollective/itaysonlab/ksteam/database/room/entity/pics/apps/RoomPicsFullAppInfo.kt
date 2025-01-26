package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import bruhcollective.itaysonlab.ksteam.database.room.entity.store.RoomStoreTag

internal data class RoomPicsFullAppInfo(
    @Embedded
    val appInfo: RoomPicsAppInfo,

    @Relation(parentColumn = "id", entityColumn = "app_id")
    val localizedAssets: List<RoomPicsAppInfoLocalizedAssets>,

    @Relation(parentColumn = "id", entityColumn = "app_id")
    val categories: List<RoomPicsAppInfoCategory>,

    @Relation(parentColumn = "id", entityColumn = "app_id")
    val descriptors: List<RoomPicsAppInfoContentDescriptor>,

    @Relation(parentColumn = "id", entityColumn = "app_id")
    val associations: List<RoomPicsAppInfoAssociation>,

    @Relation(
        parentColumn = "id", entityColumn = "id", associateBy = Junction(
            value = RoomPicsAppInfoStoreTagJunction::class,
            parentColumn = "app_id",
            entityColumn = "tag_id"
        ), entity = RoomStoreTag::class
    )
    val tags: List<RoomStoreTag>
)