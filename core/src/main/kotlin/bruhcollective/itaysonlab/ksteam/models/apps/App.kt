package bruhcollective.itaysonlab.ksteam.models.apps

import bruhcollective.itaysonlab.ksteam.models.AppId
import steam.webui.common.StoreItem

class App (
    val id: AppId
) {
    constructor(proto: StoreItem): this(
        id = AppId(proto.id ?: 0),

    )
}