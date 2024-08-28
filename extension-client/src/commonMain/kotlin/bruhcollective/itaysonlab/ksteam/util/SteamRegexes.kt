package bruhcollective.itaysonlab.ksteam.util

import kotlin.jvm.JvmInline

/**
 * Provides simple helper functions to work around common regexes found in Steam content such as links.
 */
@Suppress("RegExpRedundantEscape")
object SteamRegexes {
    private val QR_AUTH_LINK = "https:\\/\\/s\\.team\\/q\\/(.+?)\\/(.+)".toRegex()

    internal val RICH_PRESENCE_VARIABLE_TOKEN = "%(.+?)%".toRegex()
    internal val RICH_PRESENCE_LOCALE_TOKEN = "\\{(.*?)\\}".toRegex()

    /**
     * @return true if this URL is a Steam authorization link
     */
    fun isAuthorizationLink(value: String) = value matches QR_AUTH_LINK

    /**
     * @return [QrSessionData] if the link is a Steam authorization link, null otherwise
     */
    fun extractAuthorizationLinkDataOrNull(value: String): QrSessionData? {
        return QR_AUTH_LINK.matchEntire(value)?.let { match ->
            QrSessionData(
                (match.groups[1]?.value?.toInt() ?: return null) to (match.groups[2]?.value?.toULong() ?: return null)
            )
        }
    }

    @JvmInline
    value class QrSessionData (private val packed: Pair<Int, ULong>) {
        val version get() = packed.first
        val sessionId get() = packed.second
    }
}