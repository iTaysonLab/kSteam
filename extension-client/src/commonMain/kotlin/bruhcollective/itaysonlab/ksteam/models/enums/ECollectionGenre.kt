package bruhcollective.itaysonlab.ksteam.models.enums

import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionGenre.entries


enum class ECollectionGenre (val tagNumber: Int) {
    Action(19),
    Adventure(21),
    Casual(597),
    Indie(492),
    MMO(128),
    Racing(699),
    RPG(122),
    Simulation(599),
    Sports(701),
    Strategy(9);

    companion object {
        fun byNumber(i: Int) = entries.firstOrNull { it.tagNumber == i }
    }
}