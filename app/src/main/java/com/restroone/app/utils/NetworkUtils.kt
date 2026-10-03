package com.restroone.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {

    /**
     * Checks if the device has an active internet connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false

            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Helper to convert raw connection exceptions into clear, user-friendly messages.
     */
    fun formatErrorMessage(e: Throwable): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("UNAVAILABLE", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("UnknownHostException", ignoreCase = true) -> {
                "No internet connection. Please check your network and try again."
            }
            else -> e.localizedMessage ?: "Network error. Please try again."
        }
    }
}
