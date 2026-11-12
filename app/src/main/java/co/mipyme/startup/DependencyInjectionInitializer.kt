package co.mipyme.startup

import android.content.Context
import androidx.startup.Initializer
import co.mipyme.MainViewModel
import co.mipyme.screen.home.homeScreenModule
import co.mipyme.screen.welcome.welcomeScreenModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class DependencyInjectionInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(
                module { viewModelOf(::MainViewModel) },
                module { welcomeScreenModule() },
                module { homeScreenModule() },
            )
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return emptyList()
    }
}
