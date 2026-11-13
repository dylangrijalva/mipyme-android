package co.mipyme.screen.onboarding

import androidx.annotation.StringRes
import co.mipyme.contract.BusinessType
import co.mipyme.contract.CountryIsoCode

const val ONBOARDING_NAME_MAX_LENGTH = 64

// Only Central America, Mexico and Colombia are offered for now. Other countries stay in CountryIsoCode.
val ONBOARDING_SELECTABLE_COUNTRIES: Set<CountryIsoCode> = setOf(
    CountryIsoCode.NI,
    CountryIsoCode.BZ,
    CountryIsoCode.CR,
    CountryIsoCode.GT,
    CountryIsoCode.HN,
    CountryIsoCode.PA,
    CountryIsoCode.SV,
    CountryIsoCode.CO,
    CountryIsoCode.MX,
)

enum class OnboardingStep {
    NAME,
    BUSINESS_NAME,
    BUSINESS_TYPE,
    COUNTRY,
    ;

    val previous: OnboardingStep?
        get() = entries.getOrNull(ordinal - 1)

    val next: OnboardingStep?
        get() = entries.getOrNull(ordinal + 1)
}

data class OnboardingScreenUiState(
    val step: OnboardingStep = OnboardingStep.NAME,
    val name: String = "",
    val businessName: String = "",
    val businessType: BusinessType = BusinessType.CONVENIENCE_STORE,
    val country: CountryIsoCode = CountryIsoCode.NI,
    val isSubmitting: Boolean = false,
    val isSigningOut: Boolean = false,
    val isSignOutConfirmationVisible: Boolean = false,
    @param:StringRes val pendingToastRes: Int? = null,
) {
    val isBusy: Boolean
        get() = isSubmitting || isSigningOut

    val isNameTooLong: Boolean
        get() = name.trim().length > ONBOARDING_NAME_MAX_LENGTH

    val isBusinessNameTooLong: Boolean
        get() = businessName.trim().length > ONBOARDING_NAME_MAX_LENGTH

    val isStepValid: Boolean
        get() = when (step) {
            OnboardingStep.NAME -> name.trim().length in 1..ONBOARDING_NAME_MAX_LENGTH
            OnboardingStep.BUSINESS_NAME -> businessName.trim().length in 1..ONBOARDING_NAME_MAX_LENGTH
            OnboardingStep.BUSINESS_TYPE,
            OnboardingStep.COUNTRY -> true
        }

    // Creating the business is temporarily disabled, so the last step's button never enables.
    val isContinueEnabled: Boolean
        get() = isStepValid && !isBusy && step.next != null
}
