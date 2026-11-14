package co.mipyme.sync.internal

import kotlinx.coroutines.flow.Flow

internal interface ConnectivityMonitor {
    fun restoredFlow(): Flow<Unit>
}
