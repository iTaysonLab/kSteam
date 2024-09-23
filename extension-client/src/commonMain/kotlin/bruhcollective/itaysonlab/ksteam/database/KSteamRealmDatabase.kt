package bruhcollective.itaysonlab.ksteam.database

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.database.models.apps.RealmPackageLicenses
import bruhcollective.itaysonlab.ksteam.database.models.apps.RealmRichPresenceDictionary
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersona
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaLastSeen
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaStatus
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PicsAppChangeNumber
import bruhcollective.itaysonlab.ksteam.models.pics.PicsPackageChangeNumber
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.types.TypedRealmObject
import kotlin.reflect.KClass

/**
 * Manages Realm database for kSteam Client Extension. kSteam manages at least two Realm databases: shared and user.
 *
 * **Shared** database (`ks_shared.realm`) contains:
 * - cached rich presence strings
 * - PICS metadata
 *
 * **User** database (`ks_<SteamID>.realm`) contains:
 * - cached personas
 */
internal class KSteamRealmDatabase (
    private val steamClient: SteamClient
) {
    private companion object {
        const val FILE_NAME_SHARED = "ks_shared.realm"
        const val SHARED_SCHEMA_VERSION = 1L
        const val USER_SCHEMA_VERSION = 1L

        // ks_<SteamID>.realm
        fun createUserRealmFileName(id: SteamId) = "ks_$id.realm"
    }

    private var _currentUserRealmId: SteamId = steamClient.configuration.autologinSteamId
    private var _currentUserRealm: Realm? = openAutologinRealm()

    internal val sharedRealm: Realm = openSharedRealm()
    internal val isCurrentUserRealmInitialized get() = _currentUserRealm != null
    internal val currentUserRealm: Realm get() = _currentUserRealm ?: error("User Realm was not yet initialized.")

    internal fun initializeUserRealm(id: SteamId) {
        if (_currentUserRealmId != id) {
            _currentUserRealmId = id
            _currentUserRealm?.close()
            _currentUserRealm = openUserRealm(id)
        }
    }

    private fun openAutologinRealm(): Realm? {
        return steamClient.configuration.autologinSteamId.takeIf { it.isEmpty.not() }?.let { steamId ->
            openUserRealm(steamId)
        }
    }

    private fun openSharedRealm(): Realm {
        return Realm.open(
            RealmConfiguration.Builder(createSharedRealmSchema())
                .directory(steamClient.workingDirectory.toString())
                .name(FILE_NAME_SHARED)
                .schemaVersion(SHARED_SCHEMA_VERSION)
                .deleteRealmIfMigrationNeeded() // TODO: remove when this stabilizes!
                .build()
        )
    }

    private fun openUserRealm(id: SteamId): Realm {
        return Realm.open(
            RealmConfiguration.Builder(createUserRealmSchema())
                .directory(steamClient.workingDirectory.toString())
                .name(createUserRealmFileName(id))
                .schemaVersion(USER_SCHEMA_VERSION)
                .deleteRealmIfMigrationNeeded() // TODO: remove when this stabilizes!
                .build()
        )
    }

    private fun createUserRealmSchema(): Set<KClass<out TypedRealmObject>> = setOf(
        // Personas
        RealmPersona::class,
        RealmPersonaStatus::class,
        RealmPersonaLastSeen::class,

        // PICS
        RealmPackageLicenses::class,
        RealmPackageLicenses.RealmPackageLicense::class
    )

    private fun createSharedRealmSchema(): Set<KClass<out TypedRealmObject>> = setOf(
        // Rich Presence
        RealmRichPresenceDictionary::class,

        // PICS
        AppInfo::class,
        AppInfo.AppInfoCommon::class,
        AppInfo.AppInfoCommon.AppSupportedLanguageMatrix::class,
        AppInfo.AppInfoCommon.AppEula::class,
        AppInfo.AppInfoCommon.AppAssociation::class,
        AppInfo.AppInfoCommon.AppSteamDeckCompatibility::class,
        AppInfo.AppInfoCommon.AppSteamDeckCompatibility.AppSteamDeckCompatTestEntry::class,
        AppInfo.AppInfoCommon.AppInfoLibraryFullAssets::class,
        AppInfo.AppInfoCommon.AppInfoLibraryFullAssets.AppInfoLibraryFullAssetDefinition::class,
        AppInfo.AppInfoExtended::class,
        AppInfo.AppInfoAlbumMetadata::class,
        AppInfo.AppInfoAlbumMetadata.AppInfoMusicTrack::class,
        AppInfo.AppInfoAlbumMetadata.AppInfoMusicMetadata::class,
        AppInfo.AppInfoAlbumMetadata.AppInfoMusicCdnAssets::class,
        PackageInfo::class,
        PackageInfo.InfoExtended::class,
        PicsAppChangeNumber::class,
        PicsPackageChangeNumber::class

        // Library
    )
}