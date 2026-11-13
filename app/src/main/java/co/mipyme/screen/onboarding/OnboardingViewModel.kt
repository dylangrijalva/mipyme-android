package co.mipyme.screen.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.toStringResource
import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.request.OnboardAccountRequest
import co.mipyme.contract.request.OnboardLocationRequest
import co.mipyme.data.onboarding.OnboardingError
import co.mipyme.data.onboarding.OnboardingRepository
import co.mipyme.data.onboarding.mayHaveReachedServer
import co.mipyme.data.onboarding.toStringResource
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val IDEMPOTENCY_KEY = "onboarding_idempotency_key"
private const val IDEMPOTENCY_REQUEST = "onboarding_idempotency_request"

class OnboardingViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: OnboardingRepository,
    private val authService: AuthService,
) : ViewModel() {
    private var accountId: String? = currentAccount()?.id
    private var prefilledName = currentAccount()?.name.orEmpty().trim()
    private var submitJob: Job? = null

    private val _stateFlow = MutableStateFlow(
        OnboardingScreenUiState(name = prefilledName, country = defaultCountry()),
    )
    val stateFlow: StateFlow<OnboardingScreenUiState> = _stateFlow.asStateFlow()
    val state: OnboardingScreenUiState
        get() = _stateFlow.value

    init {
        viewModelScope.launch {
            authService.authStateFlow.collect { authState -> onAuthStateChanged(authState) }
        }
    }

    fun onNameChange(name: String) {
        if (state.isBusy) return
        _stateFlow.update { it.copy(name = name) }
    }

    fun onBusinessNameChange(businessName: String) {
        if (state.isBusy) return
        _stateFlow.update { it.copy(businessName = businessName) }
    }

    fun onBusinessTypeSelected(businessType: BusinessType) {
        if (state.isBusy) return
        _stateFlow.update { it.copy(businessType = businessType) }
    }

    fun onCountrySelected(country: CountryIsoCode) {
        if (state.isBusy) return
        _stateFlow.update { it.copy(country = country) }
    }

    fun onContinue() {
        if (state.isBusy || !state.isStepValid) return
        val nextStep = state.step.next
        if (nextStep == null) {
            submit()
        } else {
            _stateFlow.update { it.copy(step = nextStep) }
        }
    }

    fun onBack() {
        if (state.isBusy) return
        val previousStep = state.step.previous
        if (previousStep == null) {
            if (hasTypedInput()) {
                onSignOutClick()
            } else {
                signOut()
            }
        } else {
            _stateFlow.update { it.copy(step = previousStep) }
        }
    }

    fun onSignOutClick() {
        if (state.isBusy) return
        _stateFlow.update { it.copy(isSignOutConfirmationVisible = true) }
    }

    fun onSignOutConfirmed() {
        if (state.isBusy) return
        signOut()
    }

    fun onSignOutDismissed() {
        _stateFlow.update { it.copy(isSignOutConfirmationVisible = false) }
    }

    fun onToastShown() {
        _stateFlow.update { it.copy(pendingToastRes = null) }
    }

    private fun submit() {
        val current = state
        val request = OnboardAccountRequest(
            name = current.name.trim(),
            country = current.country,
            location = OnboardLocationRequest(
                name = current.businessName.trim(),
                businessType = current.businessType,
            ),
        )
        _stateFlow.update { it.copy(isSubmitting = true) }
        submitJob = viewModelScope.launch {
            var result = repository.onboard(request, idempotencyKeyFor(request))
            if (result.leftOrNull() == OnboardingError.IdempotencyKeyMismatch) {
                // The key was spent on a different request, so retry once with a fresh one.
                forgetIdempotencyKey()
                result = repository.onboard(request, idempotencyKeyFor(request))
            }
            _stateFlow.update { it.copy(isSubmitting = false) }
            // On success the refreshed account moves the app to its next destination.
            result.fold(
                ifLeft = { error ->
                    if (!error.mayHaveReachedServer()) {
                        forgetIdempotencyKey()
                    }
                    showError(error)
                },
                ifRight = { forgetIdempotencyKey() },
            )
        }
    }

    // A key is only reused to retry the very same request; any other request gets a fresh key,
    // because the server rejects a known key that arrives with a different body.
    private fun idempotencyKeyFor(request: OnboardAccountRequest): String {
        val fingerprint = request.toString()
        val storedKey = savedStateHandle.get<String>(IDEMPOTENCY_KEY)
        if (storedKey != null && savedStateHandle.get<String>(IDEMPOTENCY_REQUEST) == fingerprint) {
            return storedKey
        }
        return UUID.randomUUID().toString().also { key ->
            savedStateHandle[IDEMPOTENCY_KEY] = key
            savedStateHandle[IDEMPOTENCY_REQUEST] = fingerprint
        }
    }

    private fun forgetIdempotencyKey() {
        savedStateHandle.remove<String>(IDEMPOTENCY_KEY)
        savedStateHandle.remove<String>(IDEMPOTENCY_REQUEST)
    }

    // The ViewModel outlives sign-out, so another account must not inherit the previous form.
    private fun onAuthStateChanged(authState: AuthState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (authState.account.id != accountId) {
                    resetFor(authState.account)
                }
            }

            AuthState.Unauthenticated,
            AuthState.SessionExpired,
            AuthState.Blocked -> {
                if (accountId != null) {
                    resetFor(account = null)
                }
            }

            AuthState.Initializing -> Unit
        }
    }

    private fun resetFor(account: IdentityAccount?) {
        submitJob?.cancel()
        forgetIdempotencyKey()
        accountId = account?.id
        prefilledName = account?.name.orEmpty().trim()
        _stateFlow.update { OnboardingScreenUiState(name = prefilledName, country = defaultCountry()) }
    }

    private fun signOut() {
        _stateFlow.update { it.copy(isSigningOut = true, isSignOutConfirmationVisible = false) }
        viewModelScope.launch {
            val result = authService.signOut()
            _stateFlow.update { it.copy(isSigningOut = false) }
            result.onLeft { error ->
                _stateFlow.update { it.copy(pendingToastRes = error.toStringResource()) }
            }
        }
    }

    private fun hasTypedInput(): Boolean {
        return state.name.trim() != prefilledName || state.businessName.isNotBlank()
    }

    private fun showError(error: OnboardingError) {
        val stepWithIssue = (error as? OnboardingError.Validation)?.details
            ?.firstNotNullOfOrNull { detail -> detail.field.toStep() }
        _stateFlow.update {
            it.copy(
                step = stepWithIssue ?: it.step,
                pendingToastRes = error.toStringResource(),
            )
        }
    }

    private fun String?.toStep(): OnboardingStep? {
        return when (this) {
            "name" -> OnboardingStep.NAME
            "location.name" -> OnboardingStep.BUSINESS_NAME
            "location.businessType", "location.business_type" -> OnboardingStep.BUSINESS_TYPE
            "country" -> OnboardingStep.COUNTRY
            else -> null
        }
    }

    private fun currentAccount(): IdentityAccount? {
        return (authService.authStateFlow.value as? AuthState.Authenticated)?.account
    }

    private fun defaultCountry(): CountryIsoCode {
        val deviceCountry = Locale.getDefault().country
        return CountryIsoCode.entries.firstOrNull { country ->
            country in ONBOARDING_SELECTABLE_COUNTRIES && country.name.equals(deviceCountry, ignoreCase = true)
        } ?: CountryIsoCode.NI
    }
}
