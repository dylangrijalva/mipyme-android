package co.mipyme.startup

import android.content.Context
import androidx.startup.Initializer
import co.mipyme.BuildConfig
import co.mipyme.MainViewModel
import co.mipyme.R
import co.mipyme.auth.TokenProvider
import co.mipyme.auth.authModule
import co.mipyme.network.AuthTokenProvider
import co.mipyme.network.networkModule
import co.mipyme.screen.home.homeScreenModule
import co.mipyme.screen.onboarding.onboardingScreenModule
import co.mipyme.screen.welcome.welcomeScreenModule
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import timber.log.Timber

class DependencyInjectionInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(
                module { viewModelOf(::MainViewModel) },
                module { welcomeScreenModule() },
                module { onboardingScreenModule() },
                module { homeScreenModule() },
                module { single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) } },
                authModule(),
                module {
                    single<AuthTokenProvider> { AuthTokenProvider { get<TokenProvider>().getToken().getOrNull() } }
                    single<Logger> {
                        if (BuildConfig.DEBUG) {
                            object : Logger {
                                override fun log(message: String) {
                                    Timber.tag("HTTP").d(message)
                                }
                            }
                        } else {
                            Logger.EMPTY
                        }
                    }
                },
                networkModule(context.getString(R.string.mipyme_api_base_url)),
            )
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(AuthInitializer::class.java)
    }
}
