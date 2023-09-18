package com.example.getmymeeting

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class NetworkConnectivityListener(
    private val context: Context,
    private val onNetworkConnectivityChanged: (Boolean) -> Unit
) {
    private var isNetworkConnected = true
    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                isNetworkConnected = true
                notifyNetworkConnectivityChanged()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isNetworkConnected = false
                notifyNetworkConnectivityChanged()
            }
        }

    fun startListening() {
        val networkRequest = ConnectivityManager.NetworkCallback()
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateNetworkConnectivity()
    }

    fun stopListening() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun updateNetworkConnectivity() {
        isNetworkConnected = isNetworkConnected()
        notifyNetworkConnectivityChanged()
    }

    private fun isNetworkConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false
    }

    private fun notifyNetworkConnectivityChanged() {
        onNetworkConnectivityChanged.invoke(isNetworkConnected)
    }
}