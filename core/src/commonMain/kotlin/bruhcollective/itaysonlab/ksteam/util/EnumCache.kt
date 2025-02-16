package bruhcollective.itaysonlab.ksteam.util

import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult

/**
 * Caches enums <-> ID relationship
 *
 * TODO: Write a generator that creates a Map<Int, EMsg> instead
 */
internal object EnumCache {
    private val msgMap = mutableMapOf<Int, EMsg>()
    private val resultMap = mutableMapOf<Int, EResult>()
    private val languageVdfMap = mutableMapOf<String, ELanguage>()

    init {
        for (msg in EMsg.entries) {
            msgMap[msg.encoded] = msg
        }

        for (result in EResult.entries) {
            resultMap[result.encoded] = result
        }

        for (lang in ELanguage.entries) {
            languageVdfMap[lang.vdfName] = lang
        }
    }

    fun eMsg(of: Int): EMsg = msgMap[of] ?: EMsg.k_EMsgInvalid
    fun eResult(of: Int): EResult = resultMap[of] ?: EResult.Invalid
    fun eLanguage(of: String): ELanguage? = languageVdfMap[of]
}