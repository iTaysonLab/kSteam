package bruhcollective.itaysonlab.ksteam.models.library.query;

enum class KsLibraryQueryOwnerFilter {
    /**
     * Don't filter by licenses. In multi-account setups, this can lead to applications visible when the account should not access them.
     */
    None,

    /**
     * Apply default license filter. This will include owned items and additional copies available from Family Sharing.
     */
    Default,

    /**
     * Show ONLY owned applications. This will completely ignore Family Sharing items.
     */
    OwnedOnly
}