package bruhcollective.itaysonlab.ksteam.models.library.query

/**
 * Describes controller support filter.
 */
enum class KsLibraryQueryControllerSupportFilter {
    /**
     * Don't filter by controller support.
     */
    None,

    /**
     * Return any games that partially or fully support controllers.
     */
    Partial,

    /**
     * Return games that fully support controllers.
     */
    Full
}