package bruhcollective.itaysonlab.ksteam.database.entities

import bruhcollective.itaysonlab.ksteam.database.exposed.H2Compress
import bruhcollective.itaysonlab.ksteam.database.exposed.array
import bruhcollective.itaysonlab.ksteam.database.exposed.equalsAny
import bruhcollective.itaysonlab.ksteam.database.exposed.expand
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.library.DfEntry
import bruhcollective.itaysonlab.ksteam.models.library.DynamicFilters
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import io.ktor.http.*
import okio.ByteString
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

internal object PicsApp: IdTable<Int>(name = "pics_apps") {
    override val id: Column<EntityID<Int>> = integer("id").entityId()

    private val name = text("name")
    private val supportedOs = text("os_support")
    private val releaseState = text("release_state")

    private val type = enumeration<EAppType>("type")
    private val controllerSupport = enumeration<EAppControllerSupportLevel>("controller_support")
    private val deckSupport = enumeration<ESteamDeckSupport>("deck_support")

    private val masterSubAppId = integer("master_sub_app_id")

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

    private val releaseDate = long("date")
    private val steamReleaseDate = long("date_steam")

    private val picsRawData = blob("vdf")
    private val picsChangeNumber = uinteger("change_number")

    suspend fun getAppIds(db: Database) = newSuspendedTransaction(db = db) {
        PicsApp.slice(PicsApp.id).selectAll().map { AppId(it[PicsApp.id].value) }
    }

    suspend fun getVdfByAppId(db: Database, ids: List<AppId>) = newSuspendedTransaction(db = db) {
        picsRawData.expand().let { rawData ->
            PicsApp.slice(rawData).select {
                PicsApp.id inList ids.map(AppId::id)
            }.orderBy(name, SortOrder.ASC).mapNotNull { row ->
                row[rawData]
            }
        }
    }

    suspend fun getVdfByFilter(db: Database, filters: DynamicFilters) = newSuspendedTransaction(db = db) {
        picsRawData.expand().let { rawData ->
            PicsApp.slice(rawData).let { set ->
                createAllFilters(filters).let { op ->
                    if (op != null) {
                        set.select(op)
                    } else {
                        set.selectAll()
                    }
                }
            }.orderBy(name, SortOrder.ASC).mapNotNull { row ->
                row[rawData]
            }
        }
    }

    suspend fun insertAll(db: Database, info: List<PicsAppVdfRepresentation>) = newSuspendedTransaction(db = db) {
        PicsApp.batchInsert(info, shouldReturnGeneratedValues = false) { triple ->
            this[PicsApp.id] = triple.appInfo.appId
            this[name] = triple.appInfo.common.name
            this[PicsApp.type] = EAppType.values().firstOrNull { it.name.equals(triple.appInfo.common.type, ignoreCase = true) } ?: EAppType.Invalid

            this[supportedOs] = triple.appInfo.common.osList
            this[releaseState] = triple.appInfo.common.releaseState
            this[controllerSupport] = EAppControllerSupportLevel.values().firstOrNull { it.name.equals(triple.appInfo.common.controllerSupport, ignoreCase = true) } ?: EAppControllerSupportLevel.None
            this[deckSupport] = ESteamDeckSupport.values().firstOrNull { it.ordinal == triple.appInfo.common.steamDeckCompat.category } ?: ESteamDeckSupport.Unknown
            this[masterSubAppId] = triple.appInfo.common.masterSubPackageId

            this[tags] = triple.appInfo.common.tags.sortedBy { it }.toTypedArray()
            this[categories] = triple.appInfo.common.category.filter { it.value }.keys.map { it.removePrefix("category_").toInt() }.sortedBy { it }.toTypedArray()
            this[genres] = triple.appInfo.common.genres.sortedBy { it }.toTypedArray()

            this[iconFileId] = triple.appInfo.common.iconId
            this[logoFileId] = triple.appInfo.common.logoId

            this[localizedHeaderImages] = triple.appInfo.common.headerImages.joinToDatabaseString()
            this[localizedSmallCapsules] = triple.appInfo.common.smallCapsule.joinToDatabaseString()
            this[localizedNames] = triple.appInfo.common.nameLocalized.joinToDatabaseString()

            this[franchises] = triple.appInfo.common.associations.filter { it.type == "franchise" }.map { it.name }.toTypedArray()
            this[developers] = triple.appInfo.common.associations.filter { it.type == "developer" }.map { it.name }.toTypedArray()
            this[publishers] = triple.appInfo.common.associations.filter { it.type == "publisher" }.map { it.name }.toTypedArray()

            this[releaseDate] = triple.appInfo.common.releaseDate
            this[steamReleaseDate] = triple.appInfo.common.steamReleaseDate

            this[reviewScore] = EUserReviewScore.values().firstOrNull { it.ordinal == triple.appInfo.common.reviewScore } ?: EUserReviewScore.None
            this[reviewPercentage] = triple.appInfo.common.reviewPercentage.toShort()
            this[metacriticScore] = triple.appInfo.common.metacriticScore.toShort()
            this[metacriticUrl] = triple.appInfo.common.metacriticUrl
            this[picsChangeNumber] = triple.changeNumber
            this[picsRawData] = H2Compress(ExposedBlob(triple.raw.toByteArray()), useDeflate = true)
        }
    }

    // region Dynamic Filters parser

    private fun createAllFilters(filters: DynamicFilters) = listOfNotNull(
        createAppTypeFilter(filters),
        /* TODO */
        createAppFeatureFilter(filters),
        createGenreFilter(filters),
        createStoreTagFilter(filters),
        createPartnerFilter(filters),
        /* TODO Friends filter not supported in kSteam */
    ).ifEmpty {
        null
    }?.reduce { op, nextOp -> op and nextOp }

    private fun createAppTypeFilter(filters: DynamicFilters) = createFilterOp(filters.byAppType) { appType ->
        type eq appType
    }

    private fun createGenreFilter(filters: DynamicFilters) = createFilterOp(filters.byGenre) { genre ->
        genres equalsAny genre.tagNumber
    }

    private fun createStoreTagFilter(filters: DynamicFilters) = createFilterOp(filters.byStoreTag) { tag ->
        genres equalsAny tag
    }

    private fun createPartnerFilter(filters: DynamicFilters) = createFilterOp(filters.byPartner) { partner ->
        // We will assume EA Play for now
        masterSubAppId eq 1289670
    }

    private fun createAppFeatureFilter(filters: DynamicFilters) = createFilterOp(filters.byAppFeature) { feature ->
        when (feature) {
            EAppFeature.FullControllerSupport -> {
                (controllerSupport eq EAppControllerSupportLevel.Full) or (categories equalsAny 28)
            }

            EAppFeature.PartialControllerSupport -> {
                (controllerSupport eq EAppControllerSupportLevel.Full) or (categories equalsAny 28) or (controllerSupport eq EAppControllerSupportLevel.Partial) or (categories equalsAny 18)
            }

            EAppFeature.VRSupport -> {
                categories equalsAny 31
            }

            EAppFeature.TradingCards -> {
                categories equalsAny 29
            }

            EAppFeature.Workshop -> {
                categories equalsAny 30
            }

            EAppFeature.Achievements -> {
                categories equalsAny 22
            }

            EAppFeature.SinglePlayer -> {
                categories equalsAny 2
            }

            EAppFeature.MultiPlayer -> {
                (categories equalsAny 36) or (categories equalsAny 37) or (categories equalsAny 20) or (categories equalsAny 24) or (categories equalsAny 27) or (categories equalsAny 1)
            }

            EAppFeature.CoOp -> {
                (categories equalsAny 9) or (categories equalsAny 38) or (categories equalsAny 39)
            }

            EAppFeature.Cloud -> {
                categories equalsAny 23
            }

            EAppFeature.RemotePlayTogether -> {
                categories equalsAny 44
            }

            EAppFeature.SteamDeckVerified -> {
                deckSupport eq ESteamDeckSupport.Verified
            }

            EAppFeature.SteamDeckPlayable -> {
                (deckSupport eq ESteamDeckSupport.Verified) or (deckSupport eq ESteamDeckSupport.Playable)
            }

            EAppFeature.SteamDeckUnknown -> {
                deckSupport neq ESteamDeckSupport.Unsupported
            }

            EAppFeature.SteamDeckUnsupported -> {
                deckSupport eq ESteamDeckSupport.Unsupported
            }
        }
    }

    private fun <T> createFilterOp(dfEntry: DfEntry<T>, mapper: (T) -> Op<Boolean>) = dfEntry.entries.map(mapper).ifEmpty {
        null
    }?.reduce { op, nextOp ->
        if (dfEntry.acceptsUnion) {
            op or nextOp
        } else {
            op and nextOp
        }
    }

    // endregion

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