package bruhcollective.itaysonlab.ksteam.database

import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersona
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaIngameStatus
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaLastSeen
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaRelationship
import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo
import bruhcollective.itaysonlab.ksteam.models.pics.PicsAppChangeNumber
import bruhcollective.itaysonlab.ksteam.models.pics.PicsPackageChangeNumber
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.types.TypedRealmObject
import okio.Path
import kotlin.reflect.KClass

/**
 * Manages Realm database for kSteam Client Extension.
 *
 * kSteam manages its separate realm in a file "ksteam.realm" inside working directory to ensure that the library won't collide with other Realm-using libraries or apps.
 */
internal class KSteamRealmDatabase (
    workingDirectory: Path
) {
    private companion object {
        const val FILE_NAME = "ksteam.realm"
        const val SCHEMA_VERSION = 1L
    }

    internal val realm = Realm.open(
        RealmConfiguration.Builder(createRealmSchema())
            .directory(workingDirectory.toString())
            .name(FILE_NAME)
            .schemaVersion(SCHEMA_VERSION)
            .build()
    )

    private fun createRealmSchema(): Set<KClass<out TypedRealmObject>> = setOf(
        // Persona
        RealmPersona::class,
        RealmPersonaIngameStatus::class,
        RealmPersonaLastSeen::class,
        RealmPersonaRelationship::class,
        // Store

        // PICS
        AppInfo::class,
        AppInfo.AppInfoCommon::class,
        AppInfo.AppInfoCommon.AppSupportedLanguageMatrix::class,
        AppInfo.AppInfoCommon.AppEula::class,
        AppInfo.AppInfoCommon.AppAssociation::class,
        AppInfo.AppInfoCommon.AppSteamDeckCompatibility::class,
        AppInfo.AppInfoCommon.AppSteamDeckCompatibility.AppSteamDeckCompatTestEntry::class,
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