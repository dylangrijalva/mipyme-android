package co.mipyme.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

fun platformModule(): Module {
    return module {
        single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    }
}
