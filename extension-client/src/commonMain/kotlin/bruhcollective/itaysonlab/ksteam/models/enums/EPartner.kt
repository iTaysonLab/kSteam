package bruhcollective.itaysonlab.ksteam.models.enums

enum class EPartner (internal val internalValue: Double) {
    EASubscription(4e3);

    companion object {
        fun byIndex(num: Int) = entries.firstOrNull { it.internalValue == num.toDouble() }
    }
}