package co.mipyme.data.onboarding

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthService
import co.mipyme.contract.Merchant
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.contract.request.OnboardAccountRequest
import co.mipyme.network.MipymeApi
import co.mipyme.network.NetworkError
import timber.log.Timber

class OnboardingRepository(
    private val api: MipymeApi,
    private val authService: AuthService,
) {
    suspend fun onboard(
        request: OnboardAccountRequest,
        idempotencyKey: String,
    ): Either<OnboardingError, Merchant> {
        val merchant = api.accounts.onboard(request, idempotencyKey)
            .getOrElse { error -> return error.toOnboardingError().left() }
        return authService.refresh().fold(
            ifLeft = { error ->
                Timber.w("Could not refresh the account after onboarding: %s", error)
                OnboardingError.AccountRefreshFailed.left()
            },
            ifRight = { merchant.right() },
        )
    }

    private fun NetworkError.toOnboardingError(): OnboardingError {
        return when (this) {
            is NetworkError.Timeout,
            is NetworkError.NetworkUnavailable -> OnboardingError.Network

            is NetworkError.Response -> when (error.code) {
                MipymeErrorCode.VALIDATION_ERROR -> OnboardingError.Validation(error.details)
                MipymeErrorCode.CONFLICT_ERROR -> OnboardingError.EmployeeConflict
                MipymeErrorCode.IDEMPOTENCY_KEY_MISMATCH -> OnboardingError.IdempotencyKeyMismatch
                MipymeErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                MipymeErrorCode.IDEMPOTENCY_UNAVAILABLE -> OnboardingError.IdempotencyPending
                else -> OnboardingError.Unknown
            }

            is NetworkError.Serialization,
            is NetworkError.Unexpected -> OnboardingError.Unknown
        }
    }
}
