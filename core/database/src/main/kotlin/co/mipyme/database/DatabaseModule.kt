package co.mipyme.database

import androidx.room.Room
import co.mipyme.database.internal.MipymeCacheDatabase
import co.mipyme.database.internal.RoomMipymeCache
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

fun databaseModule(): Module {
    return module {
        single {
            Room.databaseBuilder(
                androidContext(),
                MipymeCacheDatabase::class.java,
                "mipyme_cache.db",
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        } onClose { it?.close() }
        single { RoomMipymeCache(get()) }
        single<MipymeCache> { get<RoomMipymeCache>() }
        single<SyncCacheWriter> { get<RoomMipymeCache>() }
    }
}
