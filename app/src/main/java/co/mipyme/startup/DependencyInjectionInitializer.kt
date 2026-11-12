package co.mipyme.startup

import android.content.Context
import androidx.startup.Initializer
import co.mipyme.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DependencyInjectionInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(appModule)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return emptyList()
    }
}
