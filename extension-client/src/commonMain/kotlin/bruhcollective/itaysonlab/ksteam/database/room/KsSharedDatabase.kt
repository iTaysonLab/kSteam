package bruhcollective.itaysonlab.ksteam.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import bruhcollective.itaysonlab.ksteam.database.room.dao.*
import bruhcollective.itaysonlab.ksteam.database.room.entity.apps.RoomRichPresenceDictionary
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsAppEntry
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsPackageEntry
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.*
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages.RoomPicsPackageInfo
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages.RoomPicsPackageInfoGrantedApp
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages.RoomPicsPackageInfoGrantedDepot
import bruhcollective.itaysonlab.ksteam.database.room.entity.store.RoomStoreTag

/**
 * A Room database for shared data like:
 * - rich presence dictionary
 * - PICS metadata
 */
@Database(
    entities = [
        // PICS - Entries
        RoomPicsAppEntry::class,
        RoomPicsPackageEntry::class,

        // PICS - App
        RoomPicsAppInfo::class,
        RoomPicsAppInfoCategory::class,
        RoomPicsAppInfoLocalizedAssets::class,
        RoomPicsAppInfoStoreTagJunction::class,
        RoomPicsAppInfoVdf::class,

        // PICS - Package
        RoomPicsPackageInfo::class,
        RoomPicsPackageInfoGrantedApp::class,
        RoomPicsPackageInfoGrantedDepot::class,

        RoomRichPresenceDictionary::class,
        RoomStoreTag::class,
    ],
    version = 1
)
internal abstract class KsSharedDatabase: RoomDatabase() {
    companion object {
        fun newInstance(builder: Builder<KsSharedDatabase>): KsSharedDatabase {
            return builder
                .fallbackToDestructiveMigration(true) // TODO: disable when schema stabilizes
                .setDriver(BundledSQLiteDriver())
                .build()
        }
    }

    abstract fun picsPackages(): RoomPicsPackageDao
    abstract fun picsApplications(): RoomPicsApplicationDao
    abstract fun picsEntries(): RoomPicsEntryDao

    abstract fun richPresenceDictionaries(): RoomRichPresenceDictionaryDao
    abstract fun storeTags(): RoomStoreTagDao
}