package co.mipyme.data.onboarding

import co.mipyme.contract.ErrorDetail

sealed interface OnboardingError {
    data object Network : OnboardingError
    data class Validation(val details: List<ErrorDetail>) : OnboardingError
    data object EmployeeConflict : OnboardingError

    // The idempotency key was already used for a different request.
    data object IdempotencyKeyMismatch : OnboardingError

    // The server could not settle the idempotent request yet, so the same key must be retried.
    data object IdempotencyPending : OnboardingError

    // The merchant was created, but the account could not be refreshed to reflect it.
    data object AccountRefreshFailed : OnboardingError
    data object Unknown : OnboardingError
}
