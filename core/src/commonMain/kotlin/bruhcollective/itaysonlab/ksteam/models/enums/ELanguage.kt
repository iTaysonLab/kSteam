package bruhcollective.itaysonlab.ksteam.models.enums

import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage.entries
import bruhcollective.itaysonlab.ksteam.util.EnumCache

// A placeholder to indicate for which languages we don't know shortened names
private const val SHORTENED_NOT_AVAILABLE = "_"

enum class ELanguage(val vdfName: String, val shortened: String) {
    English("english", "en"),
    German("german", "de"),
    French("french", "fr"),
    Italian("italian", "it"),
    Korean("koreana", "ko"),
    Spanish("spanish", "es"),
    SimpleChinese("schinese", "zh-cn"),
    TraditionalChinese("tchinese", "zh-tw"),
    Russian("russian", "ru"),
    Thai("thai", SHORTENED_NOT_AVAILABLE),
    Japanese("japanese", "jp"),
    Portuguese("portuguese", "pt"),
    Polish("polish", "pl"),
    Danish("danish", SHORTENED_NOT_AVAILABLE),
    Dutch("dutch", "nl"),
    Finnish("finnish", "fi"),
    Norwegian("norwegian", "no"),
    Swedish("swedish", SHORTENED_NOT_AVAILABLE),
    Hungarian("hungarian", SHORTENED_NOT_AVAILABLE),
    Czech("czech", "cs"),
    Romanian("romanian", SHORTENED_NOT_AVAILABLE),
    Turkish("turkish", "tr"),
    Arabic("arabic", "ar"),
    Brazilian("brazilian", "pt-br"),
    Bulgarian("bulgarian", SHORTENED_NOT_AVAILABLE),
    Greek("greek", SHORTENED_NOT_AVAILABLE),
    Ukrainian("ukrainian", "ua"),
    Latam("latam", SHORTENED_NOT_AVAILABLE),
    Vietnamese("vietnamese", "vi"),
    ScSimpleChinese("sc_schinese", SHORTENED_NOT_AVAILABLE);

    companion object {
        fun byVdf(id: String): ELanguage? = EnumCache.eLanguage(id)
        fun byShortened(id: String): ELanguage? = entries.firstOrNull { it.shortened == id }
    }
}