package co.mipyme.startup

import android.content.Context
import androidx.startup.Initializer
import co.mipyme.sync.SyncLauncher
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class SyncInitializer : Initializer<Unit>, KoinComponent {
    override fun create(context: Context) {
        get<SyncLauncher>().start()
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(DependencyInjectionInitializer::class.java)
    }
}
