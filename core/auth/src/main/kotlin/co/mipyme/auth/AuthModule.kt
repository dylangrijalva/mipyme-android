package co.mipyme.auth

import co.mipyme.auth.internal.AccountCache
import co.mipyme.auth.internal.ClerkAuthService
import co.mipyme.auth.internal.identityAccountDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun authModule(): Module {
    return module {
        // Eager, so Clerk session changes are mirrored into the cache from app start.
        single<AuthService>(createdAtStart = true) {
            ClerkAuthService(
                applicationScope = get(),
                accountCache = AccountCache(androidContext().identityAccountDataStore),
            )
        }
        single<TokenProvider> { get<AuthService>() }
    }
}
