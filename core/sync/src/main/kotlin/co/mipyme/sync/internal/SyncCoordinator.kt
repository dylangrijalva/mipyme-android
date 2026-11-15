package co.mipyme.sync.internal

import arrow.core.Either
import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.auth.SessionReadiness
import co.mipyme.auth.merchantId
import co.mipyme.database.SyncCacheWriter
import co.mipyme.sync.SyncError
import co.mipyme.sync.SyncLauncher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal class SyncCoordinator(
    private val authService: AuthService,
    private val syncRequester: SyncRequester,
    private val writer: SyncCacheWriter,
    private val foregroundMonitor: ForegroundMonitor,
    private val connectivityMonitor: ConnectivityMonitor,
    private val realtimeClient: RealtimeClient,
    private val scope: CoroutineScope,
    private val realtimeDebounce: Duration = DEFAULT_REALTIME_DEBOUNCE,
    private val retryDelays: List<Duration> = DEFAULT_RETRY_DELAYS,
) : SyncLauncher {
    override fun start() {
        scope.launch {
            var previous: AuthState? = null
            authService.authStateFlow.collectLatest { authState ->
                val wasAuthenticated = previous is AuthState.Authenticated
                previous = authState
                when (authState) {
                    is AuthState.Authenticated -> {
                        val merchantId = authState.merchantId
                        if (merchantId != null) {
                            arm(merchantId)
                        }
                    }

                    AuthState.Unauthenticated -> {
                        if (wasAuthenticated) {
                            clearCache()
                        }
                    }

                    AuthState.Initializing,
                    AuthState.SessionExpired,
                    AuthState.Blocked -> Unit
                }
            }
        }
    }

    private suspend fun clearCache() {
        withContext(NonCancellable) {
            Either.catch { writer.clearCache() }
                .onLeft { Timber.e(it, "The cache could not be cleared") }
        }
    }

    private suspend fun arm(merchantId: String) {
        Either.catch { writer.clearCacheUnlessMerchant(merchantId) }
            .onLeft { Timber.e(it, "The cache could not be checked for another merchant") }

        val forbidden = ForbiddenState()
        combine(foregroundMonitor.isForegroundFlow, authService.sessionReadinessFlow) { isForeground, readiness ->
            isForeground && readiness == SessionReadiness.Ready
        }
            .distinctUntilChanged()
            .collectLatest { isActive ->
                if (isActive) {
                    runWhileForeground(merchantId, forbidden)
                }
            }
    }

    private suspend fun runWhileForeground(merchantId: String, forbidden: ForbiddenState) {
        coroutineScope {
            val triggers = Channel<Unit>(Channel.CONFLATED)

            launch {
                realtimeClient.changesFlow(merchantId)
                    .debounce(realtimeDebounce)
                    .catch { Timber.w(it, "The realtime stream stopped") }
                    .collect { triggers.trySend(Unit) }
            }
            launch {
                connectivityMonitor.restoredFlow()
                    .catch { Timber.w(it, "The connectivity monitor stopped") }
                    .collect { triggers.trySend(Unit) }
            }
            launch {
                triggers.receiveAsFlow().collectLatest {
                    if (!forbidden.isSet) {
                        synchronizeWithRetries(forbidden)
                    }
                }
            }

            triggers.trySend(Unit)
        }
    }

    private suspend fun synchronizeWithRetries(forbidden: ForbiddenState) {
        var attempt = 0
        while (true) {
            val error = syncRequester.requestSync().leftOrNull() ?: return
            when (error) {
                SyncError.Forbidden -> {
                    forbidden.isSet = true
                    return
                }

                SyncError.Server -> {
                    val retryDelay = retryDelays.getOrNull(attempt) ?: return
                    attempt++
                    delay(retryDelay)
                }

                SyncError.Network,
                SyncError.Unauthorized,
                SyncError.Unknown -> return
            }
        }
    }

    private class ForbiddenState {
        @Volatile
        var isSet = false
    }

    private companion object {
        private val DEFAULT_REALTIME_DEBOUNCE = 500.milliseconds
        private val DEFAULT_RETRY_DELAYS = listOf(5.seconds, 15.seconds, 60.seconds)
    }
}
