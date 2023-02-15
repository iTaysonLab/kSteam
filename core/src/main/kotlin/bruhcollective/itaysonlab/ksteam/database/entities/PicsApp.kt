package bruhcollective.itaysonlab.ksteam.database.entities

import bruhcollective.itaysonlab.ksteam.database.exposed.H2Compress
import bruhcollective.itaysonlab.ksteam.database.exposed.array
import bruhcollective.itaysonlab.ksteam.database.exposed.expand
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.EAppControllerSupportLevel
import bruhcollective.itaysonlab.ksteam.models.enums.EAppType
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.EUserReviewScore
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import io.ktor.http.*
import kotlinx.datetime.Instant
import okio.ByteString
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

internal object PicsApp: IdTable<UInt>(name = "pics_apps") {
    override val id: Column<EntityID<UInt>> = uinteger("id").entityId()

    private val name = text("name")
    private val supportedOs = text("os_support")
    private val releaseState = text("release_state")

    private val type = enumeration<EAppType>("type")
    private val controllerSupport = enumeration<EAppControllerSupportLevel>("controller_support")
    private val deckSupport = enumeration<ESteamDeckSupport>("deck_support")

    private val masterSubAppId = uinteger("master_sub_app_id")

    private val tags = integer("tags").array()
    private val genres = integer("genres").array()
    private val categories = integer("categories").array()

    private val franchises = text("franchises").array()
    private val developers = text("developers").array()
    private val publishers = text("publishers").array()

    private val iconFileId = varchar("file_icon", length = 40)
    private val logoFileId = varchar("file_logo", length = 40)

    // Encoded in english=file,french=file2
    private val localizedSmallCapsules = text("loc_capsules")
    private val localizedHeaderImages = text("loc_headers")
    private val localizedNames = text("loc_names")

    private val metacriticScore = short("metacritic")
    private val metacriticUrl = text("metacritic_url")

    private val reviewScore = enumeration<EUserReviewScore>("review_score")
    private val reviewPercentage = short("review_percent")

    private val releaseDate = timestamp("date")
    private val steamReleaseDate = timestamp("date_steam")

    private val picsRawData = blob("vdf")
    private val picsChangeNumber = uinteger("change_number")

    suspend fun getAppIds(db: Database) = newSuspendedTransaction(db = db) {
        PicsApp.slice(PicsApp.id).selectAll().map { AppId(it[PicsApp.id].value) }
    }

    suspend fun getVdfByAppId(db: Database, ids: List<AppId>) = newSuspendedTransaction(db = db) {
        PicsApp.slice(picsRawData).select {
            PicsApp.id inList ids.map(AppId::id)
        }.mapNotNull {
            it[picsRawData.expand()]
        }
    }

    suspend fun insertAll(db: Database, info: List<PicsAppVdfRepresentation>) = newSuspendedTransaction(db = db) {
        PicsApp.batchInsert(info, shouldReturnGeneratedValues = false) { triple ->
            this[PicsApp.id] = triple.appInfo.appId.toUInt()
            this[name] = triple.appInfo.common.name
            this[PicsApp.type] = EAppType.values().first { it.name.equals(triple.appInfo.common.type, ignoreCase = true) }

            this[supportedOs] = triple.appInfo.common.osList
            this[releaseState] = triple.appInfo.common.releaseState
            this[controllerSupport] = EAppControllerSupportLevel.values().first { it.name.equals(triple.appInfo.common.controllerSupport, ignoreCase = true) }
            this[deckSupport] = ESteamDeckSupport.values().first { it.ordinal == triple.appInfo.common.steamDeckCompat.category }
            this[masterSubAppId] = triple.appInfo.common.masterSubPackageId.toUInt()

            this[tags] = triple.appInfo.common.tags.sortedBy { it }.toTypedArray()
            this[categories] = triple.appInfo.common.category.filter { it.value }.keys.map { it.removePrefix("category_").toInt() }.sortedBy { it }.toTypedArray()
            this[genres] = triple.appInfo.common.genres.sortedBy { it }.toTypedArray()

            this[localizedHeaderImages] = triple.appInfo.common.headerImages.joinToDatabaseString()
            this[localizedSmallCapsules] = triple.appInfo.common.smallCapsule.joinToDatabaseString()
            this[localizedNames] = triple.appInfo.common.nameLocalized.joinToDatabaseString()

            this[franchises] = triple.appInfo.common.associations.filter { it.type == "franchise" }.map { it.name }.toTypedArray()
            this[developers] = triple.appInfo.common.associations.filter { it.type == "developer" }.map { it.name }.toTypedArray()
            this[publishers] = triple.appInfo.common.associations.filter { it.type == "publisher" }.map { it.name }.toTypedArray()

            this[releaseDate] = Instant.fromEpochSeconds(triple.appInfo.common.releaseDate)
            this[steamReleaseDate] = Instant.fromEpochSeconds(triple.appInfo.common.steamReleaseDate)

            this[reviewScore] = EUserReviewScore.values().first { it.ordinal == triple.appInfo.common.reviewScore }
            this[reviewPercentage] = triple.appInfo.common.reviewPercentage.toShort()
            this[metacriticScore] = triple.appInfo.common.metacriticScore.toShort()
            this[metacriticUrl] = triple.appInfo.common.metacriticUrl
            this[picsChangeNumber] = triple.changeNumber
            this[picsRawData] = H2Compress(ExposedBlob(triple.raw.toByteArray()), useDeflate = true)
        }
    }

    private fun Map<String, String>.joinToDatabaseString(): String {
        return map {
            it.key + "=" + it.value.encodeURLParameter()
        }.joinToString(separator = ":")
    }

    @JvmInline
    value class PicsAppVdfRepresentation(private val data: Triple<AppInfo, UInt, ByteString>) {
        val appInfo: AppInfo get() = data.first
        val changeNumber: UInt get() = data.second
        val raw: ByteString get() = data.third
    }
}