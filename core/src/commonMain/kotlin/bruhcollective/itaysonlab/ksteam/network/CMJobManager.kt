package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import io.ktor.util.collections.ConcurrentMap
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
        logger.logVerbose("CMJobManager") { "Posting job ${job.id}" }
        currentJobs.put(job.id, job)
    }

    /**
     * Notifies the job manager that the job is done for the [SteamPacket].
     *
     * @return if the job was found
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun completeJob(packet: SteamPacket): Boolean {
        val jobId = packet.header.targetJobId
        val job = currentJobs[jobId] ?: return false

        logger.logVerbose("CMJobManager") { "Completing job ${job.id} [result = ${packet.result} | jobName = ${(packet.header as? SteamPacketHeader.Protobuf)?.targetJobName}]" }

        if (packet.result == EResult.OK || packet.result == EResult.Invalid) {
            runCatching {
                if (job.accept(packet)) {
                    currentJobs.remove(jobId)
                }
            }.onFailure {
                currentJobs.remove(jobId)
            }
        } else {
            logger.logVerbose("CMJobManager") { packet.payload.toHexString() }
            job.failRemote(packet.result)
            currentJobs.remove(jobId)
        }

        return true
    }

    fun dropAllJobs() {
        currentJobs.values.onEach(CMJob<*>::failDropped)
        currentJobs.clear()
    }
}