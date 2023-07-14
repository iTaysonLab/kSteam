package bruhcollective.itaysonlab.ksteam.guard.models

/**
 * A series of classes describing the logic of adding/moving a Steam Guard.
 */
sealed interface SgCreationFlowState {
    /**
     * Steam Guard creation process was not started yet.
     */
    object Idle : SgCreationFlowState

    /**
     * This means that the user has set up Steam Guard on another device.
     *
     * Confirm this request or reset Steam Guard setup.
     */
    object AlreadyHasGuard : SgCreationFlowState

    /**
     * This means an SMS was sent and a code must be provided to finish move/addition.
     */
    data class SmsSent(
        val hint: String,
        val moving: Boolean,
        internal val guardConfiguration: GuardStructure?
    ) : SgCreationFlowState

    /**
     * Steam Guard is configured on this kSteam instance.
     *
     * On this step, you should display a recovery code.
     */
    class Success(
        val recoveryCode: String
    ) : SgCreationFlowState

    class Error(
        val message: String
    ) : SgCreationFlowState
}