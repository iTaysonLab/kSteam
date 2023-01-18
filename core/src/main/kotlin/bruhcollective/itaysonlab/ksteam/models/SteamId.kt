package bruhcollective.itaysonlab.ksteam.models

import kotlin.jvm.JvmInline

@JvmInline
value class SteamId(val id: ULong) {
    companion object {
        val Empty = SteamId(0u)
    }
}