package bruhcollective.itaysonlab.ksteam.models

import bruhcollective.itaysonlab.ksteam.models.enums.EAccountType
import bruhcollective.itaysonlab.ksteam.models.enums.EUniverse
import bruhcollective.itaysonlab.ksteam.serialization.SteamIdSerializer
import kotlinx.serialization.Serializable

/**
 * A SteamID defines the unique ID of any profile on the Steam network.
 *
 * Clans are also considered as a profile, so their IDs are SteamIDs.
 *
 * kSteam provides a SteamID64 implementation.
 */
@JvmInline
@Serializable(with = SteamIdSerializer::class)
value class SteamId(val id: ULong) {
    companion object {
        val Empty = SteamId(0u)

        /**
         * Converts an account ID into a SteamID64 object.
         */
        fun fromAccountId(
            id: Long,
            instance: SteamInstance = SteamInstance.SteamUserDesktop,
            type: EAccountType = EAccountType.Individual,
            universe: EUniverse = EUniverse.Public
        ): SteamId {
            return 0L
                .setMask(0, 0xFFFFFFFF, id)
                .setMask(32, 0xFFFFF, instance.apiRepresentation.toLong())
                .setMask(52, 0xF, type.ordinal.toLong())
                .setMask(56, 0xFF, universe.ordinal.toLong())
                .let { SteamId(it.toULong()) }
        }
    }

    val debugDescription: String get() = "SteamId(id=$id, accountId=$accountId, accountInstance=$accountInstance, accountType=$accountType, accountUniverse=$accountUniverse) [user = ${isUser}, clan = ${isClan}, isEmpty = ${isEmpty}]"

    val longId get() = id.toLong()
    val accountId get() = (longId and 0xFFFFFFFF).toInt()
    val accountInstance get() = (longId ushr 32 and 0xFFFFF).let { SteamInstance.byApiRepresentation(it.toInt()) }
    val accountType get() = (longId ushr 52 and 0xF).let { EAccountType.byEncoded(it.toInt()) }
    val accountUniverse get() = (longId ushr 56 and 0xFF).let { EUniverse.byEncoded(it.toInt()) }

    val isUser get() = accountType == EAccountType.Individual
    val isClan get() = accountType == EAccountType.Clan
    val isEmpty get() = id == Empty.id

    fun equalsTo(other: SteamId) = other.id == id

    override fun toString() = id.toString()
}

private fun Long.setMask(bitOffset: Int, valueMask: Long, value: Long): Long {
    return this and (valueMask shl bitOffset).inv() or (value and valueMask shl bitOffset)
}

/**
 * Extracts account ID from SteamID64 signed long.
 */
fun Long.extractAccountIdFromSteam() = toULong().extractAccountIdFromSteam()

/**
 * Extracts account ID from SteamID64 unsigned long.
 */
fun ULong.extractAccountIdFromSteam() = SteamId(this).accountId

fun Long?.toSteamId(): SteamId = this?.toULong()?.toSteamId() ?: SteamId.Empty
fun ULong?.toSteamId(): SteamId = this?.let(::SteamId) ?: SteamId.Empty