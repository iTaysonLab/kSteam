package bruhcollective.itaysonlab.ksteam.models.enums

import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionPartner.entries


enum class ECollectionPartner (internal val internalValue: Double) {
    EASubscription(4e3);

    companion object {
        fun byIndex(num: Int) = entries.firstOrNull { it.internalValue == num.toDouble() }
    }
}