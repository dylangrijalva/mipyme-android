package co.mipyme.sync.internal

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.flatMap
import arrow.core.right
import co.mipyme.auth.AuthService
import co.mipyme.auth.SessionReadiness
import co.mipyme.auth.merchantId
import co.mipyme.contract.SyncResponse
import co.mipyme.contract.request.SyncRequest
import co.mipyme.database.MipymeCache
import co.mipyme.database.SyncCacheWriter
import co.mipyme.network.MipymeApi
import co.mipyme.network.NetworkError
import co.mipyme.sync.SyncEngine
import co.mipyme.sync.SyncError
import co.mipyme.sync.SyncStatus
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.measureTimedValue

internal class DefaultSyncEngine(
    private val api: MipymeApi,
    private val writer: SyncCacheWriter,
    private val cache: MipymeCache,
    private val authService: AuthService,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
    private val fullSyncOnLaunch: Boolean = true,
) : SyncEngine, SyncRequester {
    private val _syncStatusFlow = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatusFlow: StateFlow<SyncStatus> = _syncStatusFlow.asStateFlow()

    private val lock = Any()
    private var activeRun: CompletableDeferred<Either<SyncError, Unit>>? = null
    private var pendingRun: CompletableDeferred<Either<SyncError, Unit>>? = null
    private var activeRunMerchantId: String? = null
    private val launchSyncedMerchantId = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            authService.authStateFlow.collect {
                synchronized(lock) {
                    if (launchSyncedMerchantId.value != currentMerchantId()) {
                        launchSyncedMerchantId.value = null
                    }
                }
            }
        }
    }

    override fun lastSuccessfulSyncAtFlow(): Flow<LocalDateTime?> {
        return cache.observeSyncState().map { it?.lastSuccessfulSyncAt }.distinctUntilChanged()
    }

    override fun initialSyncCompletedFlow(merchantId: String): Flow<Boolean> {
        return combine(
            cache.observeSyncState().map { state -> state != null && state.merchantId == merchantId },
            launchSyncedMerchantId,
        ) { hasCache, launchSyncedMerchantId ->
            hasCache && (!fullSyncOnLaunch || launchSyncedMerchantId == merchantId)
        }.distinctUntilChanged()
    }

    override suspend fun synchronize(): Either<SyncError, Unit> {
        val (run, runMerchantId) = joinOrStartRun()
        val result = run.await()
        val merchantId = currentMerchantId()
        if (result.leftOrNull() == SyncError.Unauthorized && merchantId != null && merchantId != runMerchantId) {
            return joinOrStartRun().first.await()
        }
        return result
    }

    override suspend fun requestSync(): Either<SyncError, Unit> {
        return queueRun().await()
    }

    private fun joinOrStartRun(): Pair<Deferred<Either<SyncError, Unit>>, String?> {
        synchronized(lock) {
            val active = activeRun?.takeIf { it.isActive } ?: startRun()
            return active to activeRunMerchantId
        }
    }

    private fun queueRun(): Deferred<Either<SyncError, Unit>> {
        synchronized(lock) {
            val active = activeRun?.takeIf { it.isActive } ?: return startRun()
            if (isLaunchSyncPending()) {
                return active
            }
            return pendingRun ?: CompletableDeferred<Either<SyncError, Unit>>().also { pendingRun = it }
        }
    }

    private fun startRun(): CompletableDeferred<Either<SyncError, Unit>> {
        val run = CompletableDeferred<Either<SyncError, Unit>>()
        activeRun = run
        activeRunMerchantId = currentMerchantId()
        _syncStatusFlow.value = SyncStatus.Syncing
        scope.launch { drain(run) }
        return run
    }

    private suspend fun drain(first: CompletableDeferred<Either<SyncError, Unit>>) {
        var current = first
        while (true) {
            val result = try {
                execute()
            } catch (exception: CancellationException) {
                abandon(current, exception)
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "The synchronization failed unexpectedly")
                SyncError.Unknown.left()
            }
            _syncStatusFlow.value = result.fold(
                ifLeft = { SyncStatus.Failed(it) },
                ifRight = { SyncStatus.Idle },
            )
            val next = synchronized(lock) {
                pendingRun.also {
                    pendingRun = null
                    activeRun = it
                    activeRunMerchantId = currentMerchantId()
                }
            }
            current.complete(result)
            if (next == null) return
            _syncStatusFlow.value = SyncStatus.Syncing
            current = next
        }
    }

    private fun abandon(current: CompletableDeferred<Either<SyncError, Unit>>, cause: CancellationException) {
        val pending = synchronized(lock) {
            pendingRun.also {
                pendingRun = null
                activeRun = null
            }
        }
        current.completeExceptionally(cause)
        pending?.completeExceptionally(cause)
    }

    private suspend fun execute(): Either<SyncError, Unit> {
        return either {
            val readiness = awaitSessionReadiness()
            val merchantId = currentMerchantId() ?: raise(SyncError.Unauthorized)
            val savedState = writer.readSyncState()
            val launchPending = isLaunchSyncPending(merchantId)
            val cachedState = savedState?.takeIf { it.merchantId == merchantId }
            val token = if (launchPending) FULL_SYNC_TOKEN else cachedState?.syncToken ?: FULL_SYNC_TOKEN
            val synced = if (readiness == SessionReadiness.Ready) {
                fetch(token).flatMap { store(it, merchantId) }
            } else {
                SyncError.Network.left()
            }
            if (synced.isRight() && launchPending) {
                markLaunchSynced(merchantId)
            }
            if (launchPending && cachedState != null && synced.leftOrNull() == SyncError.Network) {
                if (!markLaunchSynced(merchantId)) {
                    raise(SyncError.Unauthorized)
                }
                Timber.i("The launch sync failed offline; continuing with the cached data")
                return@either
            }
            synced.bind()
        }
    }

    private suspend fun awaitSessionReadiness(): SessionReadiness {
        return authService.sessionReadinessFlow.first { it != SessionReadiness.Initializing }
    }

    private suspend fun fetch(token: String): Either<SyncError, SyncResponse> {
        val result = request(token)
        if (token != FULL_SYNC_TOKEN && result.leftOrNull()?.isSyncTokenInvalid() == true) {
            Timber.w("The saved sync token was rejected; falling back to a full sync")
            return request(FULL_SYNC_TOKEN).mapLeft { it.toSyncError() }
        }
        return result.mapLeft { it.toSyncError() }
    }

    private suspend fun request(token: String): Either<NetworkError, SyncResponse> {
        return withContext(ioDispatcher) {
            api.sync.readResources(SyncRequest(syncToken = token))
        }
    }

    private suspend fun store(response: SyncResponse, merchantId: String): Either<SyncError, Unit> {
        val timed = measureTimedValue {
            Either.catch {
                writer.apply(response, merchantId, LocalDateTime.now(clock)) { currentMerchantId() == merchantId }
            }
        }
        return timed.value.fold(
            ifLeft = { exception ->
                Timber.e(exception, "The synchronized data could not be written to the cache")
                SyncError.Unknown.left()
            },
            ifRight = { applied ->
                if (!applied) {
                    return@fold SyncError.Unauthorized.left()
                }
                if (response.fullSync) {
                    Timber.i(
                        "Full sync stored %d products, %d collections, %d customers, %d suppliers, " +
                            "%d locations in %d ms",
                        response.products.size,
                        response.collections.size,
                        response.customers.size,
                        response.suppliers.size,
                        response.locations.size,
                        timed.duration.inWholeMilliseconds,
                    )
                }
                Unit.right()
            },
        )
    }

    private fun isLaunchSyncPending(merchantId: String? = currentMerchantId()): Boolean {
        return fullSyncOnLaunch && launchSyncedMerchantId.value != merchantId
    }

    private fun markLaunchSynced(merchantId: String): Boolean {
        synchronized(lock) {
            if (currentMerchantId() != merchantId) {
                return false
            }
            launchSyncedMerchantId.value = merchantId
            return true
        }
    }

    private fun currentMerchantId(): String? {
        return authService.authStateFlow.value.merchantId
    }

    private companion object {
        private const val FULL_SYNC_TOKEN = "*"
    }
}
