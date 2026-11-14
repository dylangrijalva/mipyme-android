package co.mipyme.sync.testing

import co.mipyme.sync.internal.ConnectivityMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class FakeConnectivityMonitor : ConnectivityMonitor {
    private val restored = MutableSharedFlow<Unit>()
    var activeCollectors = 0
        private set

    override fun restoredFlow(): Flow<Unit> {
        return restored
            .onStart { activeCollectors++ }
            .onCompletion { activeCollectors-- }
    }

    suspend fun restore() {
        restored.emit(Unit)
    }
}
