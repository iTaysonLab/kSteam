package bruhcollective.itaysonlab.ksteam.models.apps

import bruhcollective.itaysonlab.ksteam.models.AppId
import steam.webui.common.StoreItem

class App (
    val id: AppId,
    val availableInCurrentCountry: Boolean,
    val name: String,
    val storeUrlPath: String,
) {
    constructor(proto: StoreItem): this(
        id = AppId(proto.id ?: 0),
        availableInCurrentCountry = proto.unvailable_for_country_restriction?.not() ?: true,
        name = proto.name.orEmpty(),
        storeUrlPath = proto.store_url_path.orEmpty()
    )
}