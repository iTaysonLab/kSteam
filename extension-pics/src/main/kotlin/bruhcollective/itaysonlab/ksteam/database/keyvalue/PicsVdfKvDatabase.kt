package bruhcollective.itaysonlab.ksteam.database.keyvalue

import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.*
import bruhcollective.itaysonlab.ksteam.models.library.DfEntry
import bruhcollective.itaysonlab.ksteam.models.library.DynamicFilters
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import bruhcollective.itaysonlab.ksteam.models.pics.type
import bruhcollective.itaysonlab.ksteam.platform.dispatchListProcessing
import bruhcollective.itaysonlab.kxvdf.RootNodeSkipperDeserializationStrategy
import bruhcollective.itaysonlab.kxvdf.Vdf
import bruhcollective.itaysonlab.kxvdf.decodeFromBufferedSource
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Buffer
import okio.ByteString.Companion.toByteString

@OptIn(ExperimentalSerializationApi::class)
internal class PicsVdfKvDatabase (
    private val db: KeyValueDatabase
) {
    object Keys {
        const val Apps = "pics.apps"
        const val Packages = "pics.packages"
    }

    private val vdfBinary = Vdf {
        ignoreUnknownKeys = true
        binaryFormat = true
        readFirstInt = true
    }

    private val vdfText = Vdf {
        ignoreUnknownKeys = true
    }

    internal val packages = SuspendableReadOnlyMap(
        initFunc = {
            parseKeyValueVdfs<PackageInfo>(vdfBinary, Keys.Packages).associateBy { it.packageId }
        }
    )

    internal val apps = SuspendableReadOnlyMap(
        initFunc = {
            parseKeyValueVdfs<AppInfo>(vdfText, Keys.Apps).associateBy { AppId(it.appId) }
        }
    )

    fun getChangeNumberFor(key: String, id: Int) = db.getLong("${key}.version.${id}")
    fun putChangeNumberFor(key: String, id: Int, data: Long) = db.putLong("${key}.version.${id}", data)
    fun putVdfFor(key: String, id: Int, data: ByteArray) = db.putByteArray("${key}.vdf.${id}", data)

    fun putPicsMetadata(key: String, entityId: Int, changeNumber: Long, buffer: ByteArray) {
        putChangeNumberFor(key, entityId, changeNumber)
        putVdfFor(key, entityId, buffer)
    }

    private suspend inline fun <reified T> parseKeyValueVdfs(vdf: Vdf, key: String) = parseVdfs<T>(vdf, db.getAllByteArrays("${key}.vdf"))

    inline fun <reified T> parseBinaryVdf(source: ByteArray) = parseVdf<T>(vdfBinary, source)
    inline fun <reified T> parseTextVdf(source: ByteArray) = parseVdf<T>(vdfText, source)

    suspend inline fun <reified T> parseVdfs(vdf: Vdf, source: List<ByteArray>): List<T> {
        return dispatchListProcessing(source) { array ->
            parseVdf(vdf, array)
        }
    }

    inline fun <reified T> parseVdf(vdf: Vdf, source: ByteArray): T? {
        return try {
            vdf.decodeFromBufferedSource<T>(RootNodeSkipperDeserializationStrategy(), Buffer().also { buffer ->
                buffer.write(source)
            })
        } catch (mfe: Exception) {
            // We try to cover almost all types, but sometimes stuff... happens
            KSteamLogging.logVerbose("Pics:Unknown", source.toByteString().hex())
            null
        }
    }

    // TODO: introduce limit
    suspend fun sortAppsByDynamicFilters(filters: DynamicFilters): Sequence<AppInfo> = withContext(Dispatchers.IO) {
        apps
            .getEntries()
            .filterByAppType(filters.byAppType)
            // .optionalFilter(filters.byPlayState)
            .filterByAppFeatures(filters.byAppFeature)
            .filterByGenres(filters.byGenre)
            .filterByStoreTag(filters.byStoreTag)
            .optionalMatchFilter(filters.byPartner) { app, feature ->
                if (feature == EPartner.EASubscription) {
                    return@optionalMatchFilter app.common.masterSubPackageId == 1289670
                } else {
                    true
                }
            }
            // .optionalMatchFilter(filters.byFriend)
    }

    private fun <T> Sequence<T>.optionalFilter(filter: DfEntry<*>, predicate: (T) -> Boolean): Sequence<T> {
        return if (filter.entries.isNotEmpty()) {
            filter(predicate)
        } else {
            this
        }
    }

    private fun <T, DfItem> Sequence<T>.optionalMatchFilter(filter: DfEntry<DfItem>, predicate: (T, DfItem) -> Boolean): Sequence<T> {
        return optionalFilter(filter) {
            val matchResults = filter.entries.map { entry ->
                predicate(it, entry)
            }

            when {
                matchResults.contains(true) && filter.acceptsUnion -> true
                matchResults.contains(false).not() && filter.acceptsUnion.not() -> true
                else -> false
            }
        }
    }

    //

    private fun Sequence<AppInfo>.filterByAppType(entry: DfEntry<EAppType>) = optionalFilter(entry) { app ->
        entry.entries.contains(app.type)
    }

    private fun Sequence<AppInfo>.filterByGenres(entry: DfEntry<EGenre>) = optionalFilter(entry) { app ->
        app.common.tags.containsAll(entry.entries.map { it.tagNumber })
    }

    private fun Sequence<AppInfo>.filterByStoreTag(entry: DfEntry<Int>) = optionalFilter(entry) { app ->
        app.common.tags.containsAll(entry.entries)
    }

    private fun Sequence<AppInfo>.filterByAppFeatures(entry: DfEntry<EAppFeature>): Sequence<AppInfo> {
        return optionalFilter(entry) { app ->
            val categories = app.common.category.filter { it.value }.keys.map { it.removePrefix("category_").toInt() }

            entry.entries.forEach { feature ->
                when (feature) {
                    EAppFeature.FullControllerSupport -> {
                        app.common.controllerSupport.equals(EAppControllerSupportLevel.Full.name, ignoreCase = true) || categories.contains(28)
                    }

                    EAppFeature.PartialControllerSupport -> {
                        app.common.controllerSupport.equals(EAppControllerSupportLevel.Full.name, ignoreCase = true) || categories.contains(28) || app.common.controllerSupport.equals(
                            EAppControllerSupportLevel.Partial.name, ignoreCase = true) || categories.contains(18)
                    }

                    EAppFeature.VRSupport -> {
                        categories.contains(31)
                    }

                    EAppFeature.TradingCards -> {
                        categories.contains(29)
                    }

                    EAppFeature.Workshop -> {
                        categories.contains(30)
                    }

                    EAppFeature.Achievements -> {
                        categories.contains(22)
                    }

                    EAppFeature.SinglePlayer -> {
                        categories.contains(2)
                    }

                    EAppFeature.MultiPlayer -> {
                        categories.any {
                            it == 36 || it == 37 || it == 20 || it == 24 || it == 27 || it == 1
                        }
                    }

                    EAppFeature.CoOp -> {
                        categories.any {
                            it == 9 || it == 38 || it == 39
                        }
                    }

                    EAppFeature.Cloud -> {
                        categories.contains(23)
                    }

                    EAppFeature.RemotePlayTogether -> {
                        categories.contains(44)
                    }

                    EAppFeature.SteamDeckVerified -> {
                        app.common.steamDeckCompat.category == ESteamDeckSupport.Verified.ordinal
                    }

                    EAppFeature.SteamDeckPlayable -> {
                        app.common.steamDeckCompat.category == ESteamDeckSupport.Verified.ordinal || app.common.steamDeckCompat.category == ESteamDeckSupport.Playable.ordinal
                    }

                    EAppFeature.SteamDeckUnknown -> {
                        app.common.steamDeckCompat.category != ESteamDeckSupport.Unsupported.ordinal
                    }

                    EAppFeature.SteamDeckUnsupported -> {
                        app.common.steamDeckCompat.category == ESteamDeckSupport.Unsupported.ordinal
                    }
                }.let {
                    if (it.not()) {
                        return@optionalFilter false
                    }
                }
            }

            true
        }
    }
}

internal class SuspendableReadOnlyMap <Key, Value> (
    private val initFunc: suspend () -> Map<Key, Value> = { emptyMap() }
) {
    private var initializationJob: Job? = null
    private val internalList = mutableMapOf<Key, Value>()

    private fun requireInitialization() = require(initializationJob != null) { "CallbackSuspendableMap must be initialized first!" }
    private suspend fun awaitInitialization() = initializationJob?.join()

    private suspend fun awaitRequiredInitialization() {
        requireInitialization()
        awaitInitialization()
    }

    suspend fun initialize() {
        if (initializationJob != null) {
            return
        }

        coroutineScope {
            initializationJob = launch {
                internalList.putAll(initFunc())
            }
        }
    }

    suspend fun get(key: Key): Value? {
        awaitRequiredInitialization()
        return internalList[key]
    }

    suspend fun getKeys(): Sequence<Key> {
        awaitRequiredInitialization()
        return internalList.keys.asSequence()
    }

    suspend fun getEntries(): Sequence<Value> {
        awaitRequiredInitialization()
        return internalList.values.asSequence()
    }
}