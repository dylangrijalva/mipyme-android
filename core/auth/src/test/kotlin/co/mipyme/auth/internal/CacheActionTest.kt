package co.mipyme.auth.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheActionTest {

    @Test
    fun not_initialized_is_never_authoritative() {
        val action = decideCacheAction(
            isInitialized = false,
            sessionState = null,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun not_initialized_with_active_session_does_not_update_cache() {
        val action = decideCacheAction(
            isInitialized = false,
            sessionState = SessionState.ACTIVE,
            hasCachedAccount = false,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun active_session_updates_cache_from_session() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = SessionState.ACTIVE,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.UpdateFromActiveSession, action)
    }

    @Test
    fun pending_session_leaves_cache_untouched() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = SessionState.PENDING,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun unknown_session_leaves_cache_untouched() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = SessionState.UNKNOWN,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun missing_session_with_cached_account_clears_as_expired() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = null,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.ClearAsExpired, action)
    }

    @Test
    fun missing_session_without_cached_account_does_nothing() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = null,
            hasCachedAccount = false,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun ended_expired_and_revoked_sessions_with_cached_account_clear_as_expired() {
        val terminalStates = listOf(
            SessionState.ABANDONED,
            SessionState.ENDED,
            SessionState.EXPIRED,
            SessionState.REMOVED,
            SessionState.REPLACED,
            SessionState.REVOKED,
        )

        terminalStates.forEach { state ->
            val action = decideCacheAction(
                isInitialized = true,
                sessionState = state,
                hasCachedAccount = true,
            )

            assertEquals("state=$state", CacheAction.ClearAsExpired, action)
        }
    }
}
