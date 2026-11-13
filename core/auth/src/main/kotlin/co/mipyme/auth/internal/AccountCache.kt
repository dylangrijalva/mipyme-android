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
import timber.log.Timber

internal class AccountCache(private val dataStore: DataStore<IdentityAccount?>) {
    private val mutex = Mutex()
    private val sessionExpired = MutableStateFlow(false)
    private val blocked = MutableStateFlow(false)

    private val loadedAccount: Flow<Pair<Boolean, IdentityAccount?>> = dataStore.data
        .map { account -> true to account }
        .onStart { emit(false to null) }
        .catch { throwable ->
            Timber.e(throwable, "Could not read the cached account")
            emit(true to null)
        }

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
        return Either.catch { dataStore.data.first() != null }
            .onLeft { throwable -> Timber.e(throwable, "Could not check the cached account") }
            .getOrNull()
    }

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
            if (dataStore.data.first() != null) {
                dataStore.updateData { null }
                sessionExpired.update { true }
            }
        }
    }

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
            }.onLeft { throwable ->
                Timber.e(throwable, "Could not clear the cached account")
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
            Either.catch { block() }
                .onLeft { throwable -> Timber.e(throwable, "Could not update the cached account") }
                .mapLeft { AuthError.Unknown("Could not update the cached account") }
        }
    }
}
