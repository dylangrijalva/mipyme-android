package co.mipyme.sync.testing

import co.mipyme.sync.internal.RealtimeClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class FakeRealtimeClient : RealtimeClient {
    private val events = MutableSharedFlow<String>()
    val subscribedMerchants = mutableListOf<String>()
    var activeCollectors = 0
        private set

    override fun changesFlow(merchantId: String): Flow<String> {
        return events
            .onStart {
                subscribedMerchants += merchantId
                activeCollectors++
            }
            .onCompletion { activeCollectors-- }
    }

    suspend fun emit(type: String) {
        events.emit(type)
    }
}
