package co.mipyme.data.onboarding

import androidx.annotation.StringRes
import co.mipyme.R
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

// Failures after which the server may already hold the outcome for the key, so a retry must
// keep it to replay that outcome.
fun OnboardingError.mayHaveReachedServer(): Boolean {
    return when (this) {
        OnboardingError.Network,
        OnboardingError.IdempotencyPending,
        OnboardingError.AccountRefreshFailed -> true

        is OnboardingError.Validation,
        OnboardingError.EmployeeConflict,
        OnboardingError.IdempotencyKeyMismatch,
        OnboardingError.Unknown -> false
    }
}

@StringRes
fun OnboardingError.toStringResource(): Int {
    return when (this) {
        OnboardingError.Network,
        OnboardingError.IdempotencyPending,
        OnboardingError.AccountRefreshFailed -> R.string.onboarding_error_network

        is OnboardingError.Validation -> R.string.onboarding_error_validation
        OnboardingError.EmployeeConflict -> R.string.onboarding_error_employee_conflict
        OnboardingError.IdempotencyKeyMismatch,
        OnboardingError.Unknown -> R.string.onboarding_error_unknown
    }
}
