package bruhcollective.itaysonlab.ksteam.database.entities

import bruhcollective.itaysonlab.ksteam.database.exposed.H2Compress
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import okio.ByteString
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

internal object PicsPackage: IdTable<UInt>(name = "pics_packages") {
    override val id: Column<EntityID<UInt>> = uinteger("id").entityId()

    private val picsRawData = blob("vdf")
    private val picsChangeNumber = uinteger("change_number")

    suspend fun selectAllAsMap(db: Database): Map<UInt, UInt> = newSuspendedTransaction(db = db) {
        PicsPackage.slice(PicsPackage.id, picsChangeNumber)
            .selectAll()
            .associate { it[PicsPackage.id].value to it[picsChangeNumber] }
    }

    suspend fun insertAll(db: Database, info: List<PicsPackageVdfRepresentation>) = newSuspendedTransaction(db = db) {
        PicsPackage.batchInsert(info, shouldReturnGeneratedValues = false) { triple ->
            this[PicsPackage.id] = triple.packageInfo.packageId.toUInt()
            this[picsChangeNumber] = triple.changeNumber
            this[picsRawData] = H2Compress(ExposedBlob(triple.raw.toByteArray()), useDeflate = true)
        }
    }

    @JvmInline
    value class PicsPackageVdfRepresentation(private val data: Triple<PackageInfo, UInt, ByteString>) {
        val packageInfo: PackageInfo get() = data.first
        val changeNumber: UInt get() = data.second
        val raw: ByteString get() = data.third
    }
}