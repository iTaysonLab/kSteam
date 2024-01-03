package bruhcollective.itaysonlab.ksteam.models.pics

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Internal "temporary" class defining change numbers for PICS app objects.
 */
internal class PicsAppChangeNumber: RealmObject {
    @PrimaryKey
    var appId: Int = 0
    var changeNumber: Int = 0
}