package co.mipyme.auth.internal

import androidx.datastore.core.DataStore
import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthError
import co.mipyme.auth.AuthState
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCacheTest {

    private val account = IdentityAccount(
        id = "user_1",
        name = "Ana Pérez",
        email = "ana@example.com",
        pictureUrl = "https://img.example.com/ana.png",
        publicMetadata = IdentityPublicMetadata(),
    )

    @Test
    fun starts_initializing_then_reflects_the_stored_account() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))

        val states = cache.authStateFlow.take(2).toList()

        assertEquals(AuthState.Initializing, states[0])
        assertEquals(AuthState.Authenticated(account), states[1])
    }

    @Test
    fun stays_initializing_until_loaded_even_when_blocked() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))
        cache.markBlocked()

        val firstState = cache.authStateFlow.first()

        assertEquals(AuthState.Initializing, firstState)
    }

    @Test
    fun blocked_takes_precedence_over_stored_account() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))

        cache.markBlocked()

        assertEquals(AuthState.Blocked, cache.settledState())
    }

    @Test
    fun stored_account_takes_precedence_over_session_expired() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)
        cache.clearAsExpired()

        dataStore.updateData { account }

        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun saves_account_while_its_session_is_active() = runBlocking {
        val dataStore = InMemoryDataStore(null)
        val cache = AccountCache(dataStore)

        val result = cache.saveIfSessionStillActive(account) { userId -> userId == account.id }

        assertEquals(Unit.right(), result)
        assertEquals(account, dataStore.current)
        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun does_not_resurrect_account_when_session_ended_during_fetch() = runBlocking {
        val dataStore = InMemoryDataStore(null)
        val cache = AccountCache(dataStore)

        cache.saveIfSessionStillActive(account) { false }

        assertNull(dataStore.current)
        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun clearing_as_expired_reports_session_expired() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)

        cache.clearAsExpired()

        assertNull(dataStore.current)
        assertEquals(AuthState.SessionExpired, cache.settledState())
    }

    @Test
    fun clearing_as_expired_without_account_stays_unauthenticated() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(null))

        cache.clearAsExpired()

        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun deliberate_sign_out_followed_by_session_end_is_not_reported_as_expired() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)

        cache.clearAfter { Unit.right() }
        cache.clearAsExpired()

        assertNull(dataStore.current)
        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun failed_sign_out_keeps_the_account() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)

        val result = cache.clearAfter { AuthError.Network.left() }

        assertEquals(AuthError.Network.left(), result)
        assertEquals(account, dataStore.current)
        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun blocked_wins_until_cleared_by_a_new_attempt() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(null))

        cache.markBlocked()
        val whileBlocked = cache.settledState()
        cache.clearAfter { Unit.right() }

        assertEquals(AuthState.Blocked, whileBlocked)
        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun new_attempt_after_expiry_clears_session_expired() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))
        cache.clearAsExpired()

        cache.clearAfter { Unit.right() }

        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun saving_an_active_session_clears_expired_and_blocked_flags() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))
        cache.clearAsExpired()
        cache.markBlocked()

        cache.saveIfSessionStillActive(account) { true }

        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun write_failure_is_returned_as_error_instead_of_thrown() = runBlocking {
        val cache = AccountCache(FailingDataStore())

        val saved = cache.saveIfSessionStillActive(account) { true }
        val cleared = cache.clearAfter { Unit.right() }

        assertTrue(saved.isLeft())
        assertTrue(cleared.isLeft())
    }

    @Test
    fun read_failure_settles_as_unauthenticated_instead_of_crashing() = runBlocking {
        val cache = AccountCache(FailingDataStore())

        assertEquals(AuthState.Unauthenticated, cache.settledState())
        assertNull(cache.hasAccount())
    }

    private suspend fun AccountCache.settledState(): AuthState {
        return authStateFlow.first { state -> state != AuthState.Initializing }
    }

    private class InMemoryDataStore(initial: IdentityAccount?) : DataStore<IdentityAccount?> {
        private val state = MutableStateFlow(initial)

        val current: IdentityAccount?
            get() = state.value

        override val data: Flow<IdentityAccount?> = state

        override suspend fun updateData(
            transform: suspend (t: IdentityAccount?) -> IdentityAccount?,
        ): IdentityAccount? {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class FailingDataStore : DataStore<IdentityAccount?> {
        override val data: Flow<IdentityAccount?> = flow { throw IOException("disk unavailable") }

        override suspend fun updateData(
            transform: suspend (t: IdentityAccount?) -> IdentityAccount?,
        ): IdentityAccount? {
            throw IOException("disk unavailable")
        }
    }
}
