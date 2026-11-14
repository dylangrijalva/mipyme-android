package co.mipyme.sync.testing

import co.mipyme.sync.internal.ForegroundMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeForegroundMonitor(initiallyForeground: Boolean = false) : ForegroundMonitor {
    val foreground = MutableStateFlow(initiallyForeground)

    override val isForegroundFlow: Flow<Boolean> = foreground
}
