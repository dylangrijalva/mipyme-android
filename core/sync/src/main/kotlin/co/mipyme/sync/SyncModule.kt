package co.mipyme.sync

import co.mipyme.sync.internal.AndroidConnectivityMonitor
import co.mipyme.sync.internal.DefaultSyncEngine
import co.mipyme.sync.internal.PusherRealtimeClient
import co.mipyme.sync.internal.ProcessLifecycleForegroundMonitor
import co.mipyme.sync.internal.SyncCoordinator
import co.mipyme.sync.internal.SyncRequester
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun syncModule(pusherKey: String, pusherCluster: String): Module {
    return module {
        single {
            DefaultSyncEngine(
                api = get(),
                writer = get(),
                cache = get(),
                authService = get(),
                scope = get(),
            )
        }
        single<SyncEngine> { get<DefaultSyncEngine>() }
        single<SyncRequester> { get<DefaultSyncEngine>() }
        single { AndroidConnectivityMonitor(androidContext()) }
        single<NetworkConnectivity> { get<AndroidConnectivityMonitor>() }
        single<SyncLauncher> {
            SyncCoordinator(
                authService = get(),
                syncRequester = get(),
                writer = get(),
                foregroundMonitor = ProcessLifecycleForegroundMonitor(),
                connectivityMonitor = get<AndroidConnectivityMonitor>(),
                realtimeClient = PusherRealtimeClient(pusherKey, pusherCluster, get()),
                scope = get(),
            )
        }
    }
}
