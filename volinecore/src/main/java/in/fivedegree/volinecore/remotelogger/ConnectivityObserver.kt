package `in`.fivedegree.volinecore.remotelogger

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Monitors network connectivity changes and triggers log queue flushing
 * when the device comes back online.
 *
 * Registers a [ConnectivityManager.NetworkCallback] that fires
 * [onConnectivityRestored] whenever a capable network becomes available.
 */
internal class ConnectivityObserver(
    context: Context,
    private val onConnectivityRestored: () -> Unit
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            onConnectivityRestored()
        }
    }

    /**
     * Start observing network changes.
     * Safe to call multiple times — only registers once.
     */
    fun start() {
        if (isRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
        } catch (_: Exception) {
            // SecurityException or other — silently ignore
        }
    }

    /**
     * Stop observing network changes.
     */
    fun stop() {
        if (!isRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
        } catch (_: Exception) {
            // Ignore if already unregistered
        }
    }

    /**
     * Check if the device currently has internet connectivity.
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
