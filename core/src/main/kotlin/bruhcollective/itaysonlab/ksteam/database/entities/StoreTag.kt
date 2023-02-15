package bruhcollective.itaysonlab.ksteam.database.entities

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

internal object StoreTag: IdTable<Int>(name = "store_tags") {
    override val id: Column<EntityID<Int>> = integer("id").entityId()
    val name = text("name")
    val localized = text("localized")
}