package co.mipyme.data.onboarding

import androidx.annotation.StringRes
import co.mipyme.R
import co.mipyme.contract.ErrorDetail

sealed interface OnboardingError {
    data object Network : OnboardingError
    data class Validation(val details: List<ErrorDetail>) : OnboardingError
    data object EmployeeConflict : OnboardingError

    data object IdempotencyKeyMismatch : OnboardingError

    data object IdempotencyPending : OnboardingError

    data object AccountRefreshFailed : OnboardingError
    data object Unknown : OnboardingError
}

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
