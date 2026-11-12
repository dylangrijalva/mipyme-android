package co.mipyme.auth.internal

import androidx.datastore.core.DataStore
import arrow.core.Either
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.contract.IdentityAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Owns the cached account (the source of truth for the auth state) plus the in-memory
// SessionExpired and Blocked flags. Every write is serialized so a deliberate sign-out is never
// reported as an expired session, and DataStore failures surface as errors instead of crashing.
internal class AccountCache(private val dataStore: DataStore<IdentityAccount?>) {
    private val mutex = Mutex()
    private val sessionExpired = MutableStateFlow(false)
    private val blocked = MutableStateFlow(false)

    private val loadedAccount: Flow<Pair<Boolean, IdentityAccount?>> = dataStore.data
        .map { account -> true to account }
        .onStart { emit(false to null) }
        .catch { emit(true to null) }

    val authStateFlow: Flow<AuthState> = combine(
        loadedAccount,
        sessionExpired,
        blocked,
    ) { (loaded, account), isSessionExpired, isBlocked ->
        deriveAuthState(
            dataStoreLoaded = loaded,
            cachedAccount = account,
            sessionExpired = isSessionExpired,
            blocked = isBlocked,
        )
    }

    suspend fun hasAccount(): Boolean? {
        return Either.catch { dataStore.data.first() != null }.getOrNull()
    }

    // The session may end while the latest user is being fetched, so the caller re-checks it
    // under the lock before the account is written.
    suspend fun saveIfSessionStillActive(
        account: IdentityAccount,
        isActiveSessionOf: (String) -> Boolean,
    ): Either<AuthError, Unit> {
        return guarded {
            if (isActiveSessionOf(account.id)) {
                dataStore.updateData { account }
                sessionExpired.update { false }
                blocked.update { false }
            }
        }
    }

    suspend fun clearAsExpired(): Either<AuthError, Unit> {
        return guarded {
            // A concurrent sign-out may already have cleared the cache.
            if (dataStore.data.first() != null) {
                dataStore.updateData { null }
                sessionExpired.update { true }
            }
        }
    }

    // Runs a provider action under the lock and clears the cache only when it succeeds, so the
    // session reaction that the action triggers finds an empty cache and stays quiet.
    suspend fun clearAfter(action: suspend () -> Either<AuthError, Unit>): Either<AuthError, Unit> {
        return mutex.withLock {
            val actionResult = action()
            if (actionResult.isLeft()) {
                return@withLock actionResult
            }
            Either.catch {
                dataStore.updateData { null }
                sessionExpired.update { false }
                blocked.update { false }
            }.mapLeft { AuthError.Unknown("Could not clear the cached account") }
        }
    }

    fun markBlocked() {
        blocked.update { true }
    }

    private fun deriveAuthState(
        dataStoreLoaded: Boolean,
        cachedAccount: IdentityAccount?,
        sessionExpired: Boolean,
        blocked: Boolean,
    ): AuthState {
        return when {
            !dataStoreLoaded -> AuthState.Initializing
            blocked -> AuthState.Blocked
            cachedAccount != null -> AuthState.Authenticated(cachedAccount)
            sessionExpired -> AuthState.SessionExpired
            else -> AuthState.Unauthenticated
        }
    }

    private suspend fun guarded(block: suspend () -> Unit): Either<AuthError, Unit> {
        return mutex.withLock {
            Either.catch { block() }.mapLeft { AuthError.Unknown("Could not update the cached account") }
        }
    }
}
