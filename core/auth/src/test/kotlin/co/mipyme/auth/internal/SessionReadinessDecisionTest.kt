package co.mipyme.auth.internal

import co.mipyme.auth.SessionReadiness
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionReadinessDecisionTest {
    @Test
    fun `an initialized clerk is ready`() {
        assertEquals(SessionReadiness.Ready, decideSessionReadiness(isInitialized = true, hasInitializationError = false))
    }

    @Test
    fun `an initialized clerk stays ready even with a stale error`() {
        assertEquals(SessionReadiness.Ready, decideSessionReadiness(isInitialized = true, hasInitializationError = true))
    }

    @Test
    fun `a refresh failure after a successful initialization stays ready`() {
        assertEquals(
            SessionReadiness.Ready,
            decideSessionReadiness(isInitialized = false, hasInitializationError = true, hasEverInitialized = true),
        )
        assertEquals(
            SessionReadiness.Ready,
            decideSessionReadiness(isInitialized = false, hasInitializationError = false, hasEverInitialized = true),
        )
    }

    @Test
    fun `a failed initialization is unavailable`() {
        assertEquals(SessionReadiness.Unavailable, decideSessionReadiness(isInitialized = false, hasInitializationError = true))
    }

    @Test
    fun `an initialization in progress is initializing`() {
        assertEquals(SessionReadiness.Initializing, decideSessionReadiness(isInitialized = false, hasInitializationError = false))
    }
}
