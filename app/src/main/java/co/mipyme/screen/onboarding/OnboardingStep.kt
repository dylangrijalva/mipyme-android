package co.mipyme.screen.onboarding

import androidx.annotation.StringRes
import co.mipyme.R

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

@StringRes
fun OnboardingStep.titleRes(): Int {
    return when (this) {
        OnboardingStep.NAME -> R.string.onboarding_name_title
        OnboardingStep.BUSINESS_NAME -> R.string.onboarding_business_name_title
        OnboardingStep.BUSINESS_TYPE -> R.string.onboarding_business_type_title
        OnboardingStep.COUNTRY -> R.string.onboarding_country_title
    }
}

@StringRes
fun OnboardingStep.subtitleRes(): Int {
    return when (this) {
        OnboardingStep.NAME -> R.string.onboarding_name_subtitle
        OnboardingStep.BUSINESS_NAME -> R.string.onboarding_business_name_subtitle
        OnboardingStep.BUSINESS_TYPE -> R.string.onboarding_business_type_subtitle
        OnboardingStep.COUNTRY -> R.string.onboarding_country_subtitle
    }
}
