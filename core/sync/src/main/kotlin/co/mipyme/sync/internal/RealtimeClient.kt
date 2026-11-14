package co.mipyme.sync.internal

import kotlinx.coroutines.flow.Flow

internal interface RealtimeClient {
    fun changesFlow(merchantId: String): Flow<String>
}
