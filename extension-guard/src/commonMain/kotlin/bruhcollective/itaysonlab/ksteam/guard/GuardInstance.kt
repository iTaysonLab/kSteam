package bruhcollective.itaysonlab.ksteam.guard

import bruhcollective.itaysonlab.ksteam.guard.clock.GuardClockContext
import bruhcollective.itaysonlab.ksteam.guard.clock.currentTime
import bruhcollective.itaysonlab.ksteam.guard.clock.currentTimeMs
import bruhcollective.itaysonlab.ksteam.guard.models.CodeModel
import bruhcollective.itaysonlab.ksteam.guard.models.ConfirmationTicket
import bruhcollective.itaysonlab.ksteam.guard.models.GuardStructure
import bruhcollective.itaysonlab.ksteam.guard.models.StaticAuthCode
import bruhcollective.itaysonlab.ksteam.models.SteamId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import kotlin.experimental.and

/**
 * A specific Steam Guard instance for a specific Steam ID.
 *
 * Generates codes, confirms trading and market sales, returns auth sessions.
 * For creating a instance if it is not created yet, refer to the [Guard] handler.
 */
class GuardInstance(
    internal val steamId: SteamId,
    private val configuration: GuardStructure,
    private val clockContext: GuardClockContext
) {
    private val sharedSecret = configuration.sharedSecret.decodeBase64() ?: error("This GuardStructure ($username) has no sharedSecret")
    private val identitySecret = configuration.identitySecret.decodeBase64() ?: error("This GuardStructure ($username) has no identitySecret")

    companion object {
        private const val Digits = 5
        private const val Period = 30 * 1000
        private val Alphabet = "23456789BCDFGHJKMNPQRTVWXY".toCharArray()
    }

    val code = flow {
        do {
            emit(generateCode())
            delay(1000L)
        } while (currentCoroutineContext().isActive)
    }

    val revocationCode get() = configuration.revocationCode
    val username get() = configuration.accountName

    private suspend fun generateCode(): CodeModel {
        val currentTime = clockContext.currentTimeMs()

        val progress = ((Period - ((currentTime) % Period)) / Period.toFloat()).coerceIn(0f..1f)
        val localDigest = Buffer().writeLong(currentTime / Period).hmacSha1(sharedSecret).toByteArray()

        val offset = (localDigest.last() and 0xf).toInt()
        val code = localDigest.copyOfRange(offset, offset + 4)
        code[0] = (0x7f and code[0].toInt()).toByte()

        return CodeModel(Triple(buildString {
            var remainingCodeInt = Buffer().write(code).readInt()
            repeat(Digits) {
                append(Alphabet[remainingCodeInt % Alphabet.size])
                remainingCodeInt /= 26
            }
        }, progress, currentTime))
    }

    suspend fun generateCodeWithTime(): StaticAuthCode {
        return generateCode().let { StaticAuthCode(it.code to it.generatedAt) }
    }

    fun sgCreateSignature(version: Int, clientId: Long): ByteString {
        return Buffer().apply {
            writeShortLe(version)
            writeLongLe(clientId)
            writeLongLe(steamId.longId)
        }.hmacSha256(sharedSecret)
    }

    fun sgCreateRevokeSignature(tokenId: Long): ByteString {
        return Buffer().apply {
            writeLong(tokenId)
        }.hmacSha256(sharedSecret)
    }

    suspend fun confirmationTicket(tag: String): ConfirmationTicket {
        val currentTime = clockContext.currentTime()

        val base64Ticket = Buffer().apply {
            writeLong(currentTime)
            writeUtf8(tag)
        }.hmacSha256(identitySecret).base64()

        return ConfirmationTicket(base64Ticket to currentTime)
    }
}