package co.mipyme.sync.internal

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
    private val connectivityManager = checkNotNull(context.getSystemService<ConnectivityManager>())

    override fun restoredFlow(): Flow<Unit> {
        return callbackFlow {
            val available = mutableSetOf<Network>()
            var suppressReplay = hasValidatedNetwork()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    synchronized(available) {
                        if (available.isEmpty() && !suppressReplay) {
                            trySend(Unit)
                        }
                        available += network
                    }
                }

                override fun onLost(network: Network) {
                    synchronized(available) {
                        available -= network
                        if (available.isEmpty()) {
                            suppressReplay = false
                        }
                    }
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    private fun hasValidatedNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
