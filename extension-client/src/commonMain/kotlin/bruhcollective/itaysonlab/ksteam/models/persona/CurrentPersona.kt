package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.platform.Immutable

/**
 * Information about the current persona signed in to the account.
 */
@Immutable
data class CurrentPersona (
    /**
     * The [SteamId] of the user.
     */
    val id: SteamId,

    /**
     * Username. This is not a Vanity URL, but rather a name that's displayed publicly.
     */
    val name: String,

    /**
     * Account flags.
     */
    val flags: AccountFlags,

    /**
     * Country code. Used for some requests.
     */
    val country: String,

    /**
     * Vanity URL.
     */
    val vanityUrl: String
) {
    companion object {
        val Unknown =
            CurrentPersona(id = SteamId(0u), name = "", flags = AccountFlags(0), country = "US", vanityUrl = "")
    }
}