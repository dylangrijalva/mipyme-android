package co.mipyme.sync.internal

import arrow.core.Either
import arrow.core.right
import co.mipyme.auth.AuthState
import co.mipyme.auth.SessionReadiness
import co.mipyme.database.SyncState
import co.mipyme.sync.SyncError
import co.mipyme.sync.SyncStatus
import co.mipyme.sync.testing.FakeAuthService
import co.mipyme.sync.testing.FakeMipymeCache
import co.mipyme.sync.testing.FakeSyncCacheWriter
import co.mipyme.sync.testing.MockApi
import co.mipyme.sync.testing.MockReply
import co.mipyme.sync.testing.authenticated
import co.mipyme.sync.testing.syncResponse
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSyncEngineTest {
    private val now = LocalDateTime.of(2026, 11, 14, 12, 0)
    private val clock = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC)
    private val savedState = SyncState("saved_token", "merch_1", LocalDateTime.of(2026, 11, 13, 9, 0))

    @Test
    fun `without a saved state the first sync asks for everything`() = runTest {
        val fixture = fixture(replies = { MockReply.Success(syncResponse("token_1")) })

        val result = fixture.engine.synchronize()

        assertEquals(Unit.right(), result)
        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `a saved token for the same merchant makes the sync incremental`() = runTest {
        val fixture = fixture(state = savedState, replies = { MockReply.Success(syncResponse("token_2", fullSync = false)) })

        fixture.engine.synchronize()

        assertEquals(listOf("saved_token"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `a saved token for another merchant is ignored`() = runTest {
        val fixture = fixture(state = savedState.copy(merchantId = "merch_other"), replies = { MockReply.Success(syncResponse()) })

        fixture.engine.synchronize()

        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `the response is written together with the merchant id and the sync time`() = runTest {
        val response = syncResponse("token_9")
        val fixture = fixture(replies = { MockReply.Success(response) })

        fixture.engine.synchronize()

        val applied = fixture.writer.applied.single()
        assertEquals(response, applied.response)
        assertEquals("merch_1", applied.merchantId)
        assertEquals(now, applied.syncedAt)
        assertEquals("token_9", fixture.writer.state?.syncToken)
    }

    @Test
    fun `a rejected token falls back to one full sync`() = runTest {
        val fixture = fixture(state = savedState, replies = { index ->
            if (index == 0) MockReply.Failure(400, "sync_token_invalid") else MockReply.Success(syncResponse("token_2"))
        })

        val result = fixture.engine.synchronize()

        assertEquals(Unit.right(), result)
        assertEquals(listOf("saved_token", "*"), fixture.mockApi.requestedTokens)
        assertEquals("token_2", fixture.writer.state?.syncToken)
    }

    @Test
    fun `a second rejection after the fallback is a normal failure`() = runTest {
        val fixture = fixture(state = savedState, replies = { MockReply.Failure(400, "sync_token_invalid") })

        val result = fixture.engine.synchronize()

        assertEquals(SyncError.Unknown, result.leftOrNull())
        assertEquals(listOf("saved_token", "*"), fixture.mockApi.requestedTokens)
        assertTrue(fixture.writer.applied.isEmpty())
    }

    @Test
    fun `a rejection of the full sync token is not retried`() = runTest {
        val fixture = fixture(replies = { MockReply.Failure(400, "sync_token_invalid") })

        fixture.engine.synchronize()

        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `server failures are reported and exposed in the status`() = runTest {
        val fixture = fixture(replies = { MockReply.Failure(503, "sync_unavailable") })

        val result = fixture.engine.synchronize()

        assertEquals(SyncError.Server, result.leftOrNull())
        assertEquals(SyncStatus.Failed(SyncError.Server), fixture.engine.syncStatusFlow.value)
        assertTrue(fixture.writer.applied.isEmpty())
    }

    @Test
    fun `a successful sync after a failure returns the status to idle`() = runTest {
        val fixture = fixture(replies = { index ->
            if (index == 0) MockReply.Failure(500, "internal_server_error") else MockReply.Success(syncResponse())
        })

        fixture.engine.synchronize()
        fixture.engine.synchronize()

        assertEquals(SyncStatus.Idle, fixture.engine.syncStatusFlow.value)
    }

    @Test
    fun `syncing without a merchant is unauthorized and sends nothing`() = runTest {
        val fixture = fixture(authState = authenticated(merchantId = null), replies = { MockReply.Success(syncResponse()) })

        val result = fixture.engine.synchronize()

        assertEquals(SyncError.Unauthorized, result.leftOrNull())
        assertTrue(fixture.mockApi.requestedTokens.isEmpty())
    }

    @Test
    fun `a cache write failure is reported as unknown`() = runTest {
        val fixture = fixture(replies = { MockReply.Success(syncResponse()) })
        fixture.writer.applyFailure = IllegalStateException("disk full")

        val result = fixture.engine.synchronize()

        assertEquals(SyncError.Unknown, result.leftOrNull())
        assertEquals(SyncStatus.Failed(SyncError.Unknown), fixture.engine.syncStatusFlow.value)
    }

    @Test
    fun `a response for a merchant that signed out meanwhile is not stored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(replies = {
            started.complete(Unit)
            gate.await()
            MockReply.Success(syncResponse())
        })
        val result = async { fixture.engine.synchronize() }
        started.await()

        fixture.authService.authState.value = AuthState.Unauthenticated
        gate.complete(Unit)

        assertEquals(SyncError.Unauthorized, result.await().leftOrNull())
        assertTrue(fixture.writer.applied.isEmpty())
    }

    @Test
    fun `triggers during a running sync collapse into exactly one more run`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(replies = { index ->
            if (index == 0) {
                started.complete(Unit)
                gate.await()
            }
            MockReply.Success(syncResponse("token_$index"))
        })
        val first = async { fixture.engine.requestSync() }
        started.await()
        val queued = List(3) { async { fixture.engine.requestSync() } }

        gate.complete(Unit)
        (listOf(first) + queued).awaitAll()

        assertEquals(2, fixture.mockApi.requestedTokens.size)
        assertEquals(listOf("token_0", "token_1"), fixture.writer.applied.map { it.response.syncToken })
    }

    @Test
    fun `an explicit synchronize joins the running sync instead of queueing another`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(replies = {
            started.complete(Unit)
            gate.await()
            MockReply.Success(syncResponse())
        })
        val first = async { fixture.engine.requestSync() }
        started.await()
        val joined = async { fixture.engine.synchronize() }

        gate.complete(Unit)
        first.await()
        joined.await()

        assertEquals(1, fixture.mockApi.requestedTokens.size)
    }

    @Test
    fun `cancelling the caller does not cancel the running sync`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(replies = {
            started.complete(Unit)
            gate.await()
            MockReply.Success(syncResponse("token_1"))
        })
        val caller = launch { fixture.engine.synchronize() }
        started.await()

        caller.cancelAndJoin()
        gate.complete(Unit)
        val joined = fixture.engine.synchronize()

        assertEquals(Unit.right(), joined)
        assertEquals(1, fixture.mockApi.requestedTokens.size)
        assertEquals(1, fixture.writer.applied.size)
    }

    @Test
    fun `a retry right after a failed run starts a new run instead of joining the finished one`() = runTest {
        val fixture = fixture(replies = { index ->
            if (index == 0) MockReply.Failure(500, "internal_server_error") else MockReply.Success(syncResponse())
        })
        var second: Either<SyncError, Unit>? = null

        val caller = launch(Dispatchers.Unconfined) {
            fixture.engine.synchronize()
            second = fixture.engine.synchronize()
        }
        caller.join()

        assertEquals(Unit.right(), second)
        assertEquals(2, fixture.mockApi.requestedTokens.size)
    }

    @Test
    fun `synchronize after an account switch does not report the previous merchant's failure`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(replies = { index ->
            if (index == 0) {
                started.complete(Unit)
                gate.await()
            }
            MockReply.Success(syncResponse("token_$index"))
        })
        val first = async { fixture.engine.requestSync() }
        started.await()
        fixture.authService.authState.value = authenticated(merchantId = "merch_2")
        val joined = async { fixture.engine.synchronize() }

        gate.complete(Unit)

        assertEquals(SyncError.Unauthorized, first.await().leftOrNull())
        assertEquals(Unit.right(), joined.await())
        assertEquals("merch_2", fixture.writer.applied.single().merchantId)
    }

    @Test
    fun `the launch sync is full even when a token is saved`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse("token_2")) })

        fixture.engine.synchronize()

        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `a background request while the launch sync is pending runs the full sync`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse("token_$it")) })

        val background = async { fixture.engine.requestSync() }
        val screen = async { fixture.engine.synchronize() }
        background.await()
        screen.await()

        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `syncs after the launch sync stay incremental`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse("token_$it")) })

        fixture.engine.synchronize()
        fixture.engine.requestSync()

        assertEquals(listOf("*", "token_0"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `the launch sync completes the initial sync only after it succeeds`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.cache.syncState.value = savedState
        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_1").first())

        fixture.engine.synchronize()

        assertTrue(fixture.engine.initialSyncCompletedFlow("merch_1").first())
    }

    @Test
    fun `a network failure on the launch sync continues with the cache`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Offline })
        fixture.cache.syncState.value = savedState

        val result = fixture.engine.synchronize()

        assertEquals(Unit.right(), result)
        assertTrue(fixture.engine.initialSyncCompletedFlow("merch_1").first())
        assertTrue(fixture.writer.applied.isEmpty())
        assertEquals(SyncStatus.Idle, fixture.engine.syncStatusFlow.value)
    }

    @Test
    fun `a timeout on the launch sync also continues with the cache`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Timeout })
        fixture.cache.syncState.value = savedState

        val result = fixture.engine.synchronize()

        assertEquals(Unit.right(), result)
        assertTrue(fixture.engine.initialSyncCompletedFlow("merch_1").first())
    }

    @Test
    fun `the completed flow goes from false to true after a launch success`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.cache.syncState.value = savedState
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch { fixture.engine.initialSyncCompletedFlow("merch_1").toList(emitted) }
        runCurrent()

        fixture.engine.synchronize()
        runCurrent()

        assertEquals(listOf(false, true), emitted)
    }

    @Test
    fun `the completed flow goes from false to true after an offline auto continue`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Offline })
        fixture.cache.syncState.value = savedState
        val emitted = mutableListOf<Boolean>()
        backgroundScope.launch { fixture.engine.initialSyncCompletedFlow("merch_1").toList(emitted) }
        runCurrent()

        fixture.engine.synchronize()
        runCurrent()

        assertEquals(listOf(false, true), emitted)
    }

    @Test
    fun `without the launch full sync the cache alone completes the initial sync and the saved token is used`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = false, replies = { MockReply.Success(syncResponse("token_2", fullSync = false)) })
        fixture.cache.syncState.value = savedState

        assertTrue(fixture.engine.initialSyncCompletedFlow("merch_1").first())
        fixture.engine.synchronize()

        assertEquals(listOf("saved_token"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `synchronize then requestSync during the launch sync sends one request`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = {
            started.complete(Unit)
            gate.await()
            MockReply.Success(syncResponse())
        })
        val screen = async { fixture.engine.synchronize() }
        started.await()
        val background = async { fixture.engine.requestSync() }

        gate.complete(Unit)
        screen.await()
        background.await()

        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `requestSync then synchronize during the launch sync sends one request`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = {
            started.complete(Unit)
            gate.await()
            MockReply.Success(syncResponse())
        })
        val background = async { fixture.engine.requestSync() }
        started.await()
        val screen = async { fixture.engine.synchronize() }

        gate.complete(Unit)
        background.await()
        screen.await()

        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `a session that expires before the launch flag is set must redo the launch sync`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.cache.syncState.value = savedState
        fixture.writer.afterApply = { fixture.authService.authState.value = AuthState.SessionExpired }

        fixture.engine.synchronize()
        fixture.writer.afterApply = null
        fixture.authService.authState.value = authenticated()
        runCurrent()

        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_1").first())
        fixture.engine.synchronize()
        assertEquals(listOf("*", "*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `a merchant switch during an offline launch fetch retries for the new merchant`() = runTest {
        lateinit var authService: FakeAuthService
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { index ->
            if (index == 0) {
                authService.authState.value = authenticated(merchantId = "merch_2")
                MockReply.Offline
            } else {
                MockReply.Success(syncResponse("token_2"))
            }
        })
        authService = fixture.authService
        fixture.cache.syncState.value = savedState

        val result = fixture.engine.synchronize()

        assertEquals(Unit.right(), result)
        assertEquals(listOf("*", "*"), fixture.mockApi.requestedTokens)
        assertEquals("merch_2", fixture.writer.applied.single().merchantId)
    }

    @Test
    fun `a network failure on the launch sync without a cache is an error`() = runTest {
        val fixture = fixture(fullSyncOnLaunch = true, replies = { MockReply.Offline })

        val result = fixture.engine.synchronize()

        assertEquals(SyncError.Network, result.leftOrNull())
        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_1").first())
    }

    @Test
    fun `a server failure on the launch sync with a cache is still an error`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Failure(503, "sync_unavailable") })
        fixture.cache.syncState.value = savedState

        val result = fixture.engine.synchronize()

        assertEquals(SyncError.Server, result.leftOrNull())
        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_1").first())
        assertEquals(savedState, fixture.writer.state)
    }

    @Test
    fun `signing out and back in requires the launch sync again`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.cache.syncState.value = savedState
        fixture.engine.synchronize()
        assertTrue(fixture.engine.initialSyncCompletedFlow("merch_1").first())

        fixture.authService.authState.value = AuthState.Unauthenticated
        runCurrent()
        fixture.authService.authState.value = authenticated()
        runCurrent()

        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_1").first())
    }

    @Test
    fun `a sync waits for the session to become ready and then sends one request`() = runTest {
        val fixture = fixture(replies = { MockReply.Success(syncResponse()) })
        fixture.authService.sessionReadiness.value = SessionReadiness.Initializing
        val result = async { fixture.engine.synchronize() }
        runCurrent()
        assertTrue(fixture.mockApi.requestedTokens.isEmpty())
        assertEquals(SyncStatus.Syncing, fixture.engine.syncStatusFlow.value)

        fixture.authService.sessionReadiness.value = SessionReadiness.Ready

        assertEquals(Unit.right(), result.await())
        assertEquals(1, fixture.mockApi.requestedTokens.size)
    }

    @Test
    fun `a cold start with synchronize then requestSync waiting for readiness sends one full request`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.authService.sessionReadiness.value = SessionReadiness.Initializing
        val screen = async { fixture.engine.synchronize() }
        runCurrent()
        val background = async { fixture.engine.requestSync() }
        runCurrent()

        fixture.authService.sessionReadiness.value = SessionReadiness.Ready

        assertEquals(Unit.right(), screen.await())
        assertEquals(screen.await(), background.await())
        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `a cold start with requestSync then synchronize waiting for readiness sends one full request`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.authService.sessionReadiness.value = SessionReadiness.Initializing
        val background = async { fixture.engine.requestSync() }
        runCurrent()
        val screen = async { fixture.engine.synchronize() }
        runCurrent()

        fixture.authService.sessionReadiness.value = SessionReadiness.Ready

        assertEquals(Unit.right(), background.await())
        assertEquals(background.await(), screen.await())
        assertEquals(listOf("*"), fixture.mockApi.requestedTokens)
    }

    @Test
    fun `an unavailable session on the launch sync continues with the cache without a request`() = runTest {
        val fixture = fixture(state = savedState, fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.cache.syncState.value = savedState
        fixture.authService.sessionReadiness.value = SessionReadiness.Initializing
        val result = async { fixture.engine.synchronize() }
        runCurrent()

        fixture.authService.sessionReadiness.value = SessionReadiness.Unavailable

        assertEquals(Unit.right(), result.await())
        assertTrue(fixture.mockApi.requestedTokens.isEmpty())
        assertTrue(fixture.engine.initialSyncCompletedFlow("merch_1").first())
    }

    @Test
    fun `an unavailable session without a cache is a network error`() = runTest {
        val fixture = fixture(fullSyncOnLaunch = true, replies = { MockReply.Success(syncResponse()) })
        fixture.authService.sessionReadiness.value = SessionReadiness.Unavailable

        val result = fixture.engine.synchronize()

        assertEquals(SyncError.Network, result.leftOrNull())
        assertTrue(fixture.mockApi.requestedTokens.isEmpty())
        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_1").first())
    }

    @Test
    fun `status reports syncing while a run is in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        val fixture = fixture(replies = {
            started.complete(Unit)
            gate.await()
            MockReply.Success(syncResponse())
        })
        val result = async { fixture.engine.synchronize() }
        started.await()

        assertEquals(SyncStatus.Syncing, fixture.engine.syncStatusFlow.value)

        gate.complete(Unit)
        result.await()
        assertEquals(SyncStatus.Idle, fixture.engine.syncStatusFlow.value)
    }

    @Test
    fun `the initial sync is complete only for the merchant that has a saved state`() = runTest {
        val fixture = fixture(replies = { MockReply.Success(syncResponse()) })
        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_1").first())

        fixture.cache.syncState.value = savedState
        assertTrue(fixture.engine.initialSyncCompletedFlow("merch_1").first())
        assertFalse(fixture.engine.initialSyncCompletedFlow("merch_other").first())
    }

    @Test
    fun `the last successful sync time comes from the saved state`() = runTest {
        val fixture = fixture(replies = { MockReply.Success(syncResponse()) })
        assertEquals(null, fixture.engine.lastSuccessfulSyncAtFlow().first())

        fixture.cache.syncState.value = savedState

        assertEquals(savedState.lastSuccessfulSyncAt, fixture.engine.lastSuccessfulSyncAtFlow().first())
    }

    private class Fixture(
        val engine: DefaultSyncEngine,
        val cache: FakeMipymeCache,
        val writer: FakeSyncCacheWriter,
        val mockApi: MockApi,
        val authService: FakeAuthService,
    )

    private fun TestScope.fixture(
        state: SyncState? = null,
        authState: AuthState = authenticated(),
        fullSyncOnLaunch: Boolean = false,
        replies: suspend (Int) -> MockReply,
    ): Fixture {
        val writer = FakeSyncCacheWriter(state)
        val mockApi = MockApi(replies)
        val authService = FakeAuthService(authState)
        val cache = FakeMipymeCache()
        val engine = DefaultSyncEngine(
            api = mockApi.api,
            writer = writer,
            cache = cache,
            authService = authService,
            scope = backgroundScope,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            clock = clock,
            fullSyncOnLaunch = fullSyncOnLaunch,
        )
        return Fixture(engine, cache, writer, mockApi, authService)
    }
}
