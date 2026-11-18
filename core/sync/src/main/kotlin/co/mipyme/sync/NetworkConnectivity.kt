package co.mipyme.sync

import kotlinx.coroutines.flow.Flow

interface NetworkConnectivity {
    val isOnline: Boolean

    fun observeOnline(): Flow<Boolean>
}
