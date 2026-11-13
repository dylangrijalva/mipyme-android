package co.mipyme.network

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

// Resolves `AuthTokenProvider` and Ktor's `Logger` from the Koin graph.
fun networkModule(baseUrl: String): Module {
    return module {
        single { MipymeApi(baseUrl, get(), get()) } onClose { it?.close() }
    }
}
