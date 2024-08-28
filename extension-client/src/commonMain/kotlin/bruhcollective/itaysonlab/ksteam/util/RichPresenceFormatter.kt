package bruhcollective.itaysonlab.ksteam.util

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.database.models.apps.RealmRichPresenceDictionary
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import io.github.reactivecircus.cache4k.Cache
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.types.RealmInstant
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Formats Steam rich presence response into a human-readable string, respecting all variables and chosen languages.
 */
class RichPresenceFormatter internal constructor(
    private val steamClient: ExtendedSteamClient,
    private val database: KSteamRealmDatabase
) {
    companion object {
        // How many dictionaries we can save in memory?
        private const val IN_MEM_CACHE_SIZE = 128L

        // How long the dictionary should be stored until a new one is requested from the network?
        // TODO: this should be tweaked to be cached as long as possible
        private val IN_DISK_CACHE_DURATION = 24.hours
    }

    private val rpLocalizationCache = Cache.Builder<RpCacheKey, Map<String, String>>()
        .maximumCacheSize(IN_MEM_CACHE_SIZE)
        .build()

    suspend fun formatRichPresenceText(
        appid: AppId,
        language: ELanguage,
        presence: Map<String, String>,
    ): String {
        val localizationTokens = getLocalizedRichPresence(appid, language)

        return presence["steam_display"]?.let(localizationTokens::get)?.replace(SteamRegexes.RICH_PRESENCE_VARIABLE_TOKEN) { match ->
            presence[match.groupValues[1]].orEmpty()
        }?.replace(SteamRegexes.RICH_PRESENCE_LOCALE_TOKEN) { match ->
            localizationTokens[match.groupValues[1]].orEmpty()
        }.orEmpty()
    }

    private suspend fun getLocalizedRichPresence(
        appId: AppId,
        language: ELanguage
    ): Map<String, String> {
        val cacheKey = RpCacheKey(appId, language)

        return rpLocalizationCache.get(key = cacheKey) {
            // 1. We check the database for cache availability
            val inDatabase = database.sharedRealm.query<RealmRichPresenceDictionary>("id == $0", cacheKey.key)
                .first()
                .find()

            if (inDatabase != null && inDatabase.expiresAt > RealmInstant.now()) {
                // Dictionary is not expired, we can use it
                return@get inDatabase.strings
            }

            // Otherwise, go for the network!
            return@get steamClient.store.getRichPresenceLocalization(appId = appId.value).orEmpty().also { strings ->
                // Save in the database for speeding access after process death
                database.sharedRealm.write {
                    val cached = RealmRichPresenceDictionary()
                    cached.id = cacheKey.key
                    cached.expiresAt = RealmInstant.from(epochSeconds = (Clock.System.now() + IN_DISK_CACHE_DURATION).epochSeconds, nanosecondAdjustment = 0)
                    cached.strings.putAll(strings)
                    copyToRealm(cached, updatePolicy = UpdatePolicy.ALL)
                }
            }
        }
    }

    private data class RpCacheKey (
        val appId: AppId,
        val language: ELanguage
    ) {
        val key: String = "${appId.value}_${language.vdfName}"
    }
}