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
    fun `starts initializing then reflects the stored account`() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))

        val states = cache.authStateFlow.take(2).toList()

        assertEquals(AuthState.Initializing, states[0])
        assertEquals(AuthState.Authenticated(account), states[1])
    }

    @Test
    fun `stays initializing until loaded even when blocked`() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))
        cache.markBlocked()

        val firstState = cache.authStateFlow.first()

        assertEquals(AuthState.Initializing, firstState)
    }

    @Test
    fun `blocked takes precedence over stored account`() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))

        cache.markBlocked()

        assertEquals(AuthState.Blocked, cache.settledState())
    }

    @Test
    fun `stored account takes precedence over session expired`() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)
        cache.clearAsExpired()

        dataStore.updateData { account }

        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun `saves account while its session is active`() = runBlocking {
        val dataStore = InMemoryDataStore(null)
        val cache = AccountCache(dataStore)

        val result = cache.saveIfSessionStillActive(account) { userId -> userId == account.id }

        assertEquals(Unit.right(), result)
        assertEquals(account, dataStore.current)
        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun `does not resurrect account when session ended during fetch`() = runBlocking {
        val dataStore = InMemoryDataStore(null)
        val cache = AccountCache(dataStore)

        cache.saveIfSessionStillActive(account) { false }

        assertNull(dataStore.current)
        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun `clearing as expired reports session expired`() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)

        cache.clearAsExpired()

        assertNull(dataStore.current)
        assertEquals(AuthState.SessionExpired, cache.settledState())
    }

    @Test
    fun `clearing as expired without account stays unauthenticated`() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(null))

        cache.clearAsExpired()

        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun `deliberate sign out followed by session end is not reported as expired`() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)

        cache.clearAfter { Unit.right() }
        cache.clearAsExpired()

        assertNull(dataStore.current)
        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun `failed sign out keeps the account`() = runBlocking {
        val dataStore = InMemoryDataStore(account)
        val cache = AccountCache(dataStore)

        val result = cache.clearAfter { AuthError.Network.left() }

        assertEquals(AuthError.Network.left(), result)
        assertEquals(account, dataStore.current)
        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun `blocked wins until cleared by a new attempt`() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(null))

        cache.markBlocked()
        val whileBlocked = cache.settledState()
        cache.clearAfter { Unit.right() }

        assertEquals(AuthState.Blocked, whileBlocked)
        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun `new attempt after expiry clears session expired`() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))
        cache.clearAsExpired()

        cache.clearAfter { Unit.right() }

        assertEquals(AuthState.Unauthenticated, cache.settledState())
    }

    @Test
    fun `saving an active session clears expired and blocked flags`() = runBlocking {
        val cache = AccountCache(InMemoryDataStore(account))
        cache.clearAsExpired()
        cache.markBlocked()

        cache.saveIfSessionStillActive(account) { true }

        assertEquals(AuthState.Authenticated(account), cache.settledState())
    }

    @Test
    fun `write failure is returned as error instead of thrown`() = runBlocking {
        val cache = AccountCache(FailingDataStore())

        val saved = cache.saveIfSessionStillActive(account) { true }
        val cleared = cache.clearAfter { Unit.right() }

        assertTrue(saved.isLeft())
        assertTrue(cleared.isLeft())
    }

    @Test
    fun `read failure settles as unauthenticated instead of crashing`() = runBlocking {
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
