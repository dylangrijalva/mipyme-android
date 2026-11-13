package co.mipyme.di

import co.mipyme.mainScreenModule
import co.mipyme.screen.home.homeScreenModule
import co.mipyme.screen.onboarding.onboardingScreenModule
import co.mipyme.screen.welcome.welcomeScreenModule
import org.koin.core.module.Module
import org.koin.dsl.module

fun screenModule(): Module {
    return module {
        mainScreenModule()
        welcomeScreenModule()
        onboardingScreenModule()
        homeScreenModule()
    }
}
