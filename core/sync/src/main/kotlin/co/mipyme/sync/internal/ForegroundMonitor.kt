package co.mipyme.sync.internal

import kotlinx.coroutines.flow.Flow

internal interface ForegroundMonitor {
    val isForegroundFlow: Flow<Boolean>
}
