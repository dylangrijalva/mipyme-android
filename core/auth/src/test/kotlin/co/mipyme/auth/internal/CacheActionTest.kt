package co.mipyme.auth.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheActionTest {

    @Test
    fun `not initialized is never authoritative`() {
        val action = decideCacheAction(
            isInitialized = false,
            sessionState = null,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun `not initialized with active session does not update cache`() {
        val action = decideCacheAction(
            isInitialized = false,
            sessionState = SessionState.ACTIVE,
            hasCachedAccount = false,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun `active session updates cache from session`() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = SessionState.ACTIVE,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.UpdateFromActiveSession, action)
    }

    @Test
    fun `pending session leaves cache untouched`() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = SessionState.PENDING,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun `unknown session leaves cache untouched`() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = SessionState.UNKNOWN,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun `missing session with cached account clears as expired`() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = null,
            hasCachedAccount = true,
        )

        assertEquals(CacheAction.ClearAsExpired, action)
    }

    @Test
    fun `missing session without cached account does nothing`() {
        val action = decideCacheAction(
            isInitialized = true,
            sessionState = null,
            hasCachedAccount = false,
        )

        assertEquals(CacheAction.None, action)
    }

    @Test
    fun `ended expired and revoked sessions with cached account clear as expired`() {
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
