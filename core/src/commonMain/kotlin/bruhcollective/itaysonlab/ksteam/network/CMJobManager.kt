package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobDroppedException
import io.ktor.util.collections.*
import kotlinx.atomicfu.atomic

/**
 * A job manager for CM clients.
 *
 * This does not actually send requests, but rather manages the queue of incoming requests.
 */
internal class CMJobManager (
    val logger: Logger
) {
    private val jobIdRef = atomic(0L)
    private val currentJobs = ConcurrentMap<CMJobId, CMJob<*>>()

    /**
     * Creates a [CMJobId].
     */
    fun createJobId(): CMJobId {
        return jobIdRef.incrementAndGet()
    }

    /**
     * Adds a job into a job manager.
     */
    fun postJob(job: CMJob<*>) {
        currentJobs.put(job.information.id, job)
    }

    /**
     * Notifies the job manager that the job is done for the [SteamPacket].
     *
     * @return if the job was found
     */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun completeJob(packet: SteamPacket): Boolean {
        val jobId = packet.header.targetJobId
        val job = currentJobs[jobId] ?: return false

        logger.logVerbose("CMJobManager") { "completing ${job.information} [result = ${packet.result}]" }

        if (packet.result == EResult.OK || packet.result == EResult.Invalid) {
            runCatching {
                if (job.accept(packet)) {
                    currentJobs.remove(jobId)
                }
            }.onFailure {
                logger.logVerbose("CMJobManager") { "exception when completing ${job.information} [message = ${it.message}]" }
                currentJobs.remove(jobId)
            }
        } else {
            job.failRemote(packet.result)
            currentJobs.remove(jobId)
        }

        return true
    }

    fun dropAllJobs(reason: CMJobDroppedException.Reason) {
        currentJobs.values.onEach { it.failDropped(reason) }
        currentJobs.clear()
    }
}