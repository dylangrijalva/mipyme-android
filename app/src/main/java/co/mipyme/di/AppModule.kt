package co.mipyme.di

import co.mipyme.MainViewModel
import co.mipyme.screen.home.homeScreenModule
import co.mipyme.screen.welcome.welcomeScreenModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::MainViewModel)
    welcomeScreenModule()
    homeScreenModule()
}
