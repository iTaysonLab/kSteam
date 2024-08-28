package bruhcollective.itaysonlab.ksteam.database.models.apps

import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.types.RealmDictionary
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class RealmRichPresenceDictionary() : RealmObject {
    /**
     * Combined Language + AppID string in order to easily find the dictionary.
     */
    @PrimaryKey
    var id: String = "" // [appid]_[language]

    /**
     * Marks the expiration date of this rich presence dictionary.
     *
     * This should be relatively close to the write date in order to keep in time with actual application updates.
     */
    var expiresAt: RealmInstant = RealmInstant.now()

    /**
     * Actual rich presence strings. This should be directly unmodified response from Steam.
     */
    var strings: RealmDictionary<String> = realmDictionaryOf()
}