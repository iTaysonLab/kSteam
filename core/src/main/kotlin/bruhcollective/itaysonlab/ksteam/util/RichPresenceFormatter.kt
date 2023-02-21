package bruhcollective.itaysonlab.ksteam.util

object RichPresenceFormatter {
    private val REGEX = """
        \{#%(.*?)%\}
    """.trimIndent().toRegex()
    fun formatRp(steamRp: Map<String, String>, localizationTokens: Map<String, String>): FormattedRichPresence {
        val display = steamRp["steam_display"]
        val groupSize = steamRp["steam_player_group_size"]?.toIntOrNull() ?: 0

        return FormattedRichPresence(groupSize to display?.let {
            localizationTokens[it]
        }?.replace(REGEX) { match ->
            localizationTokens["#${steamRp[match.groupValues[1]]}"].orEmpty()
        }.orEmpty())
    }

    @JvmInline
    value class FormattedRichPresence(private val packed: Pair<Int, String>) {
        val groupSize get() = packed.first
        val formattedString get() = packed.second
    }
}