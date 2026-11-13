package co.mipyme.startup

import android.content.Context
import androidx.startup.Initializer
import co.mipyme.R
import co.mipyme.auth.TokenProvider
import co.mipyme.auth.authModule
import co.mipyme.di.platformModule
import co.mipyme.di.screenModule
import co.mipyme.network.AuthTokenProvider
import co.mipyme.network.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class DependencyInjectionInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(
                platformModule(),
                screenModule(),
                authModule(),
                module {
                    single<AuthTokenProvider> { AuthTokenProvider { get<TokenProvider>().getToken().getOrNull() } }
                },
                networkModule(context.getString(R.string.mipyme_api_base_url)),
            )
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(AuthInitializer::class.java)
    }
}
