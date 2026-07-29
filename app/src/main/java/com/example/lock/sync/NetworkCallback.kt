package com.example.lock.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest

/**
 * U12 / AND-005: poll on network-reconnect so a device that was offline picks up
 * the latest valid command as soon as it regains connectivity (Release Gate:
 * "offline device executes latest valid command on reconnect").
 */
class NetworkCallback(private val context: Context) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        SyncWorker.pollNow(context)
    }

    fun register() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            this,
        )
    }
}
