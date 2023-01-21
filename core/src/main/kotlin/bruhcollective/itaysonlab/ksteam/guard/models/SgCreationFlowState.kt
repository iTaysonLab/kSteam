package bruhcollective.itaysonlab.ksteam.guard.models

import bruhcollective.itaysonlab.ksteam.proto.GuardConfiguration

/**
 * A series of classes describing the logic of adding/moving a Steam Guard.
 */
sealed class SgCreationFlowState {
    /**
     * Display a full-screen progress.
     */
    object TryingToAdd: SgCreationFlowState()

    /**
     * This means that the user has set up Steam Guard on another device.
     */
    class AlreadyHasGuard(
        val isProcessingRequest: Boolean = false
    ): SgCreationFlowState()

    /**
     * This means an SMS was sent and a code must be provided to finish move/addition.
     */
    data class SmsSent(
        val hint: String,
        val returnedBecauseOfError: Boolean,
        val moving: Boolean,
        val guardConfiguration: GuardConfiguration?
    ): SgCreationFlowState()

    /**
     * SMS is confirmed, processing the request
     */
    object Processing: SgCreationFlowState()

    /**
     * Steam Guard is configured on this kSteam instance.
     *
     * On this step, you should display a recovery code.
     */
    class Success(
        val recoveryCode: String
    ): SgCreationFlowState()

    class Error(
        val message: String
    ): SgCreationFlowState()
}