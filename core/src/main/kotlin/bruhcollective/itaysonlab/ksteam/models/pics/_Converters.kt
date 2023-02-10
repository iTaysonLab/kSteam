package bruhcollective.itaysonlab.ksteam.models.pics

import bruhcollective.itaysonlab.ksteam.persist.PicsApp
import bruhcollective.itaysonlab.ksteam.pics.model.AppInfo
import bruhcollective.itaysonlab.kxvdf.RootNodeSkipperDeserializationStrategy
import bruhcollective.itaysonlab.kxvdf.Vdf
import bruhcollective.itaysonlab.kxvdf.decodeFromBufferedSource
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Buffer

@OptIn(ExperimentalSerializationApi::class)
private val vdfAppInfo = Vdf {
    ignoreUnknownKeys = true
}

internal fun AppInfo.asPicsDb(changeNumber: Long, rawData: ByteArray) = PicsApp(
    id = appId.toLong(),
    name = common.name,
    type = common.type,
    supportedOs = common.osList,
    released = common.releaseState,
    controllerSupport = common.controllerSupport,
    deckSupportCategory = common.steamDeckCompat.category.toLong(),
    masterSubAppId = common.masterSubPackageId.toLong(),
    tags = common.tags.joinToDatabaseString(),
    categories = common.category.filter { it.value }.keys.joinToDatabaseString { it.removePrefix("category_") },
    genres = common.genres.joinToDatabaseString(),
    imageHeaderFileName = common.headerImages.joinToDatabaseString(),
    imageCapsuleFileName = common.smallCapsule.joinToDatabaseString(),
    localizedNames = common.nameLocalized.joinToDatabaseString(),
    franchise = common.associations.filter { it.type == "franchise" }.joinToString { it.name },
    developers = common.associations.filter { it.type == "developer" }.joinToString { it.name },
    publishers = common.associations.filter { it.type == "publisher" }.joinToString { it.name },
    releaseDate = common.releaseDate,
    steamReleaseDate = common.steamReleaseDate,
    reviewScore = common.reviewScore.toLong(),
    reviewPercentage = common.reviewPercentage.toLong(),
    metacriticScore = common.metacriticScore.toLong(),
    metacriticUrl = common.metacriticUrl,
    picsChangeNumber = changeNumber,
    picsRawData = rawData
)

@OptIn(ExperimentalSerializationApi::class)
internal fun PicsApp.asAppInfo(): AppInfo {
    return vdfAppInfo.decodeFromBufferedSource<AppInfo>(RootNodeSkipperDeserializationStrategy(), Buffer().write(picsRawData))
}

private fun <T> Iterable<T>.joinToDatabaseString(transform: ((T) -> CharSequence)? = null): String {
    return joinToString(separator = ":", postfix = ":", transform = transform).let {
        if (it.first() == ':') {
            ""
        } else {
            it
        }
    }
}

private fun Map<String, String>.joinToDatabaseString(): String {
    return map {
        it.key + "=" + it.value.encodeURLParameter()
    }.joinToString(separator = ":")
}