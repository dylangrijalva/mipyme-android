package co.mipyme.di

import co.mipyme.mainScreenModule
import co.mipyme.screen.home.homeScreenModule
import co.mipyme.screen.onboarding.onboardingScreenModule
import co.mipyme.screen.sign_in_with_email.signInWithEmailScreenModule
import co.mipyme.screen.verify_otp_code.verifyOtpCodeScreenModule
import co.mipyme.screen.welcome.welcomeScreenModule
import org.koin.core.module.Module
import org.koin.dsl.module

fun screenModule(): Module {
    return module {
        mainScreenModule()
        welcomeScreenModule()
        signInWithEmailScreenModule()
        verifyOtpCodeScreenModule()
        onboardingScreenModule()
        homeScreenModule()
    }
}
