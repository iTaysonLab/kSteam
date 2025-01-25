package bruhcollective.itaysonlab.ksteam.models.guard

import bruhcollective.itaysonlab.ksteam.models.enums.EResult

/**
 * A series of classes describing the result of adding/moving a Steam Guard.
 */
sealed interface SgCreationResult {
    /**
     * This means that the user has set up Steam Guard on another device.
     *
     * Confirm this request or reset Steam Guard setup.
     */
    data object AlreadyHasGuard : SgCreationResult

    /**
     * This means an SMS or an email was sent and a code must be provided to finish move/addition.
     *
     * The [guardConfiguration] field is an intermediate structure that is used when creating Steam Guard for the first time.
     */
    data class AwaitingConfirmation(
        val hint: String = "",
        val moving: Boolean = false,
        val guardConfiguration: GuardStructure? = null,
        val isEmail: Boolean = false,
    ): SgCreationResult

    /**
     * An error occurred when creating or moving Steam Guard.
     */
    data class Error(
        val code: EResult
    ): SgCreationResult
}

sealed interface SgDeletionResult {
    data object UnsupportedOperation: SgDeletionResult

    data object Success: SgDeletionResult

    data class InvalidCode (
        val attemptsLeft: Int
    ) : SgDeletionResult

    data class Error(
        val code: EResult
    ): SgDeletionResult
}