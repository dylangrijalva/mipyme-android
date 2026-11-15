package co.mipyme.sync.internal

import arrow.core.left
import arrow.core.right
import co.mipyme.auth.AuthState
import co.mipyme.auth.SessionReadiness
import co.mipyme.database.SyncState
import co.mipyme.sync.SyncError
import co.mipyme.sync.testing.FakeAuthService
import co.mipyme.sync.testing.FakeConnectivityMonitor
import co.mipyme.sync.testing.FakeForegroundMonitor
import co.mipyme.sync.testing.FakeRealtimeClient
import co.mipyme.sync.testing.FakeSyncCacheWriter
import co.mipyme.sync.testing.FakeSyncRequester
import co.mipyme.sync.testing.authenticated
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinatorTest {
    @Test
    fun `an authenticated merchant in the foreground syncs and listens for changes`() = runTest {
        val fixture = armed()

        assertEquals(1, fixture.requester.calls)
        assertEquals(listOf("merch_1"), fixture.realtime.subscribedMerchants)
        assertEquals(1, fixture.realtime.activeCollectors)
        assertEquals(1, fixture.connectivity.activeCollectors)
    }

    @Test
    fun `nothing syncs or connects while the session is initializing`() = runTest {
        val fixture = armed(readiness = SessionReadiness.Initializing)

        assertEquals(0, fixture.requester.calls)
        assertEquals(0, fixture.realtime.activeCollectors)
        assertEquals(0, fixture.connectivity.activeCollectors)
    }

    @Test
    fun `nothing syncs or connects while the session is unavailable`() = runTest {
        val fixture = armed(readiness = SessionReadiness.Unavailable)

        assertEquals(0, fixture.requester.calls)
        assertEquals(0, fixture.realtime.activeCollectors)
    }

    @Test
    fun `becoming ready later connects and runs one sync`() = runTest {
        val fixture = armed(readiness = SessionReadiness.Initializing)

        fixture.auth.sessionReadiness.value = SessionReadiness.Ready
        runCurrent()

        assertEquals(1, fixture.requester.calls)
        assertEquals(listOf("merch_1"), fixture.realtime.subscribedMerchants)
        assertEquals(1, fixture.realtime.activeCollectors)
    }

    @Test
    fun `nothing happens while the app is in the background`() = runTest {
        val fixture = armed(foreground = false)

        assertEquals(0, fixture.requester.calls)
        assertEquals(0, fixture.realtime.activeCollectors)
        assertEquals(0, fixture.connectivity.activeCollectors)
    }

    @Test
    fun `an account without a merchant is not armed`() = runTest {
        val fixture = armed(authState = authenticated(merchantId = null))

        assertEquals(0, fixture.requester.calls)
        assertEquals(0, fixture.writer.clearCalls)
    }

    @Test
    fun `going to the background disconnects and returning syncs again`() = runTest {
        val fixture = armed()

        fixture.foreground.foreground.value = false
        runCurrent()
        assertEquals(0, fixture.realtime.activeCollectors)
        assertEquals(0, fixture.connectivity.activeCollectors)

        fixture.foreground.foreground.value = true
        runCurrent()
        assertEquals(2, fixture.requester.calls)
        assertEquals(1, fixture.realtime.activeCollectors)
        assertEquals(1, fixture.connectivity.activeCollectors)
    }

    @Test
    fun `restored connectivity triggers a sync`() = runTest {
        val fixture = armed()

        fixture.connectivity.restore()
        runCurrent()

        assertEquals(2, fixture.requester.calls)
    }

    @Test
    fun `realtime changes trigger one sync after the debounce`() = runTest {
        val fixture = armed()

        fixture.realtime.emit("product")
        advanceTimeBy(499.milliseconds)
        runCurrent()
        assertEquals(1, fixture.requester.calls)

        advanceTimeBy(2.milliseconds)
        runCurrent()
        assertEquals(2, fixture.requester.calls)
    }

    @Test
    fun `a burst of realtime changes coalesces into one sync`() = runTest {
        val fixture = armed()

        repeat(5) { index ->
            fixture.realtime.emit("sale")
            advanceTimeBy(100.milliseconds)
        }
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(2, fixture.requester.calls)
    }

    @Test
    fun `an employee change triggers a sync`() = runTest {
        val fixture = armed()

        fixture.realtime.emit("employee")
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(2, fixture.requester.calls)
    }

    @Test
    fun `an unknown realtime type still triggers a sync`() = runTest {
        val fixture = armed()

        fixture.realtime.emit("future_resource")
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(2, fixture.requester.calls)
    }

    @Test
    fun `a cache from another merchant is cleared when arming`() = runTest {
        val writer = FakeSyncCacheWriter(SyncState("token", "merch_other", null))

        val fixture = armed(writer = writer)

        assertEquals(1, fixture.writer.clearCalls)
        assertEquals(1, fixture.requester.calls)
    }

    @Test
    fun `a cache from the same merchant is kept when arming`() = runTest {
        val writer = FakeSyncCacheWriter(SyncState("token", "merch_1", null))

        val fixture = armed(writer = writer)

        assertEquals(0, fixture.writer.clearCalls)
    }

    @Test
    fun `an empty cache is not cleared when arming`() = runTest {
        val fixture = armed()

        assertEquals(0, fixture.writer.clearCalls)
    }

    @Test
    fun `signing out clears the cache and disarms`() = runTest {
        val fixture = armed()

        fixture.auth.authState.value = AuthState.Unauthenticated
        runCurrent()
        fixture.connectivity.restore()
        runCurrent()

        assertEquals(1, fixture.writer.clearCalls)
        assertEquals(1, fixture.requester.calls)
        assertEquals(0, fixture.realtime.activeCollectors)
    }

    @Test
    fun `a cold start without a session does not clear the cache`() = runTest {
        val fixture = armed(authState = AuthState.Unauthenticated)

        assertEquals(0, fixture.writer.clearCalls)
    }

    @Test
    fun `expiry then re-login as the same merchant keeps the cache`() = runTest {
        val fixture = armed(writer = FakeSyncCacheWriter(SyncState("token", "merch_1", null)))

        fixture.auth.authState.value = AuthState.SessionExpired
        runCurrent()
        fixture.auth.authState.value = AuthState.Unauthenticated
        runCurrent()
        fixture.auth.authState.value = authenticated()
        runCurrent()

        assertEquals(0, fixture.writer.clearCalls)
        assertEquals("token", fixture.writer.state?.syncToken)
    }

    @Test
    fun `an expired or blocked session keeps the cache and disarms`() = runTest {
        listOf(AuthState.SessionExpired, AuthState.Blocked).forEach { state ->
            val fixture = armed()

            fixture.auth.authState.value = state
            runCurrent()
            fixture.connectivity.restore()
            runCurrent()

            assertEquals("state=$state", 0, fixture.writer.clearCalls)
            assertEquals("state=$state", 1, fixture.requester.calls)
            assertEquals("state=$state", 0, fixture.realtime.activeCollectors)
        }
    }

    @Test
    fun `a server failure retries with a growing backoff and stops after three retries`() = runTest {
        val fixture = armed(results = List(10) { SyncError.Server.left() })

        advanceTimeBy(4.seconds)
        runCurrent()
        assertEquals(1, fixture.requester.calls)

        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(2, fixture.requester.calls)

        advanceTimeBy(14.seconds)
        runCurrent()
        assertEquals(2, fixture.requester.calls)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(3, fixture.requester.calls)

        advanceTimeBy(59.seconds)
        runCurrent()
        assertEquals(3, fixture.requester.calls)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(4, fixture.requester.calls)

        advanceTimeBy(10.minutesInSeconds())
        runCurrent()
        assertEquals(4, fixture.requester.calls)
    }

    @Test
    fun `a retry that succeeds ends the backoff`() = runTest {
        val fixture = armed(results = listOf(SyncError.Server.left(), Unit.right()))

        advanceTimeBy(5.seconds)
        runCurrent()
        advanceTimeBy(10.minutesInSeconds())
        runCurrent()

        assertEquals(2, fixture.requester.calls)
    }

    @Test
    fun `network unauthorized and unknown failures are not retried`() = runTest {
        listOf(SyncError.Network, SyncError.Unauthorized, SyncError.Unknown).forEach { error ->
            val fixture = armed(results = listOf(error.left()))

            advanceTimeBy(10.minutesInSeconds())
            runCurrent()

            assertEquals("error=$error", 1, fixture.requester.calls)
        }
    }

    @Test
    fun `a new trigger during the backoff syncs immediately`() = runTest {
        val fixture = armed(results = listOf(SyncError.Server.left(), Unit.right()))

        fixture.connectivity.restore()
        runCurrent()

        assertEquals(2, fixture.requester.calls)
        advanceTimeBy(10.minutesInSeconds())
        runCurrent()
        assertEquals(2, fixture.requester.calls)
    }

    @Test
    fun `a forbidden response stops syncing until the auth state changes`() = runTest {
        val fixture = armed(results = listOf(SyncError.Forbidden.left()))

        fixture.connectivity.restore()
        runCurrent()
        fixture.foreground.foreground.value = false
        runCurrent()
        fixture.foreground.foreground.value = true
        runCurrent()
        assertEquals(1, fixture.requester.calls)

        fixture.auth.authState.value = authenticated(merchantId = "merch_1", accountId = "user_2")
        runCurrent()
        assertEquals(2, fixture.requester.calls)
    }

    private fun Int.minutesInSeconds() = (this * 60).seconds

    private class Fixture(
        val auth: FakeAuthService,
        val requester: FakeSyncRequester,
        val writer: FakeSyncCacheWriter,
        val foreground: FakeForegroundMonitor,
        val connectivity: FakeConnectivityMonitor,
        val realtime: FakeRealtimeClient,
    )

    private fun TestScope.armed(
        authState: AuthState = authenticated(),
        foreground: Boolean = true,
        readiness: SessionReadiness = SessionReadiness.Ready,
        writer: FakeSyncCacheWriter = FakeSyncCacheWriter(),
        results: List<arrow.core.Either<SyncError, Unit>> = emptyList(),
    ): Fixture {
        val auth = FakeAuthService(authState)
        auth.sessionReadiness.value = readiness
        val requester = FakeSyncRequester().apply { this.results.addAll(results) }
        val foregroundMonitor = FakeForegroundMonitor(foreground)
        val connectivity = FakeConnectivityMonitor()
        val realtime = FakeRealtimeClient()
        SyncCoordinator(
            authService = auth,
            syncRequester = requester,
            writer = writer,
            foregroundMonitor = foregroundMonitor,
            connectivityMonitor = connectivity,
            realtimeClient = realtime,
            scope = backgroundScope,
        ).start()
        runCurrent()
        return Fixture(auth, requester, writer, foregroundMonitor, connectivity, realtime)
    }
}
