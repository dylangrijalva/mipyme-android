package co.mipyme.startup

import android.content.Context
import androidx.startup.Initializer
import co.mipyme.R
import co.mipyme.auth.TokenProvider
import co.mipyme.auth.authModule
import co.mipyme.database.databaseModule
import co.mipyme.di.platformModule
import co.mipyme.di.screenModule
import co.mipyme.network.AuthTokenProvider
import co.mipyme.network.networkModule
import co.mipyme.sync.syncModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class DependencyInjectionInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(
                platformModule(),
                screenModule(),
                authModule(),
                module {
                    single<AuthTokenProvider> {
                        AuthTokenProvider {
                            get<TokenProvider>().getToken()
                                .onLeft { error -> Timber.w("Could not get a bearer token: %s", error) }
                                .getOrNull()
                        }
                    }
                },
                networkModule(context.getString(R.string.mipyme_api_base_url)),
                databaseModule(),
                syncModule(
                    pusherKey = context.getString(R.string.pusher_api_key),
                    pusherCluster = context.getString(R.string.pusher_cluster),
                ),
            )
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(AuthInitializer::class.java)
    }
}
