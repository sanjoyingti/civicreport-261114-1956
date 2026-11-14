package com.arin.civicreport.ui.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkChecker {
    suspend fun isOnline(context: Context): Boolean = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return@withContext false
        val caps = cm.getNetworkCapabilities(network) ?: return@withContext false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
