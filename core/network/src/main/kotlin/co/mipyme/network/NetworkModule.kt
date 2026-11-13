package co.mipyme.network

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

fun networkModule(baseUrl: String): Module {
    return module {
        single {
            MipymeApi(baseUrl, get())
        } onClose { it?.close() }
    }
}
