package bruhcollective.itaysonlab.ksteam.guard

import bruhcollective.itaysonlab.ksteam.guard.clock.GuardClockContext
import bruhcollective.itaysonlab.ksteam.guard.clock.currentTime
import bruhcollective.itaysonlab.ksteam.guard.clock.currentTimeMs
import bruhcollective.itaysonlab.ksteam.guard.models.CodeModel
import bruhcollective.itaysonlab.ksteam.guard.models.ConfirmationTicket
import bruhcollective.itaysonlab.ksteam.guard.models.StaticAuthCode
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.proto.GuardConfiguration
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.buffer
import okio.sink
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.min

/**
 * A specific Steam Guard instance for a specific Steam ID.
 *
 * Generates codes, confirms trading and market sales, returns auth sessions.
 * For creating a instance if it is not created yet, refer to the [Guard] handler.
 */
class GuardInstance (
    private val steamId: SteamId,
    private val configuration: GuardConfiguration,
    private val clockContext: GuardClockContext
) {
    companion object {
        internal const val AlgorithmTotp = "HmacSHA1"
        internal const val AlgorithmConfirmation = "HmacSHA256"

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

    val revocationCode get() = configuration.revocation_code
    val username get() = configuration.account_name

    private val secretKey = SecretKeySpec(configuration.shared_secret.toByteArray(), AlgorithmTotp)
    private val secretKeyIdentity = SecretKeySpec(configuration.identity_secret.toByteArray(), AlgorithmTotp)

    private val digest = Mac.getInstance(AlgorithmTotp).also { it.init(secretKey) }
    private val digestIdentity = Mac.getInstance(AlgorithmTotp).also { it.init(secretKeyIdentity) }

    private suspend fun generateCode(): CodeModel {
        val currentTime = clockContext.currentTimeMs()

        val progress = ((Period - ((currentTime) % Period)) / Period.toFloat()).coerceIn(0f..1f)
        val localDigest = digest.doFinal(ByteBuffer.allocate(8).putLong(currentTime / Period).array())

        val offset = (localDigest.last() and 0xf).toInt()
        val code = localDigest.copyOfRange(offset, offset + 4)
        code[0] = (0x7f and code[0].toInt()).toByte()

        return CodeModel(Triple(buildString {
            var remainingCodeInt = ByteBuffer.wrap(code).int
            repeat(Digits) {
                append(Alphabet[remainingCodeInt % Alphabet.size])
                remainingCodeInt /= 26
            }
        }, progress, currentTime))
    }

    private fun digestSha256(msg: ByteArray): ByteArray {
        val localKey = SecretKeySpec(configuration.shared_secret.toByteArray(), AlgorithmConfirmation)
        val localDigest = Mac.getInstance(AlgorithmConfirmation).also { it.init(localKey) }
        return localDigest.doFinal(msg)
    }

    suspend fun generateCodeWithTime(): StaticAuthCode {
        return generateCode().let { StaticAuthCode(it.code to it.generatedAt) }
    }

    fun sgCreateSignature(version: Int, clientId: Long): ByteString {
        return ByteArrayOutputStream(2 + 8 + 8).apply {
            sink().buffer().use { sink ->
                sink.writeShortLe(version)
                sink.writeLongLe(clientId)
                sink.writeLongLe(steamId.longId)
            }
        }.toByteArray().let(this::digestSha256).toByteString()
    }

    fun sgCreateRevokeSignature(tokenId: Long): ByteString {
        return ByteArrayOutputStream(20).apply {
            sink().buffer().use { sink ->
                sink.writeLong(tokenId)
            }
        }.toByteArray().let(this::digestSha256).toByteString()
    }

    suspend fun confirmationTicket(tag: String): ConfirmationTicket {
        val currentTime = clockContext.currentTime()

        val base64Ticket = ByteArrayOutputStream(min(tag.length, 32) + 8).apply {
            sink().buffer().use { sink ->
                sink.writeLong(currentTime)
                sink.writeUtf8(tag)
            }
        }.toByteArray().let { arr ->
            digestIdentity.doFinal(arr)
        }.toByteString().base64()

        return ConfirmationTicket(base64Ticket to currentTime)
    }
}