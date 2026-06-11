package com.yashwanthsurabhi.shielddns.rules

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.yashwanthsurabhi.shielddns.data.store.AppSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkConditionWatcher(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val networkType: Flow<NetworkType> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentType())
            }

            override fun onLost(network: Network) {
                trySend(currentType())
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(currentType())
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        trySend(currentType())
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    fun currentType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            else -> NetworkType.OTHER
        }
    }

    fun shouldBlockOnCurrentNetwork(settings: AppSettings): Boolean {
        if (!settings.blockOnlyMobile && !settings.blockOnlyWifi) return true
        return when (currentType()) {
            NetworkType.MOBILE -> settings.blockOnlyMobile
            NetworkType.WIFI -> settings.blockOnlyWifi
            else -> !settings.blockOnlyMobile && !settings.blockOnlyWifi
        }
    }
}

enum class NetworkType {
    WIFI,
    MOBILE,
    OTHER,
    NONE,
}
