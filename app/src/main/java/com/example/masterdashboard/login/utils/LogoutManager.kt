package com.example.masterdashboard.login.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.masterdashboard.login.ActivityVisitorPortal
import com.example.masterdashboard.popup_manager.LogoutPopupManager
import com.google.firebase.auth.FirebaseAuth
import kotlin.getValue

class LogoutManager(private val context: Context) {

    companion object {
        private const val TAG = "LogoutManager"
    }

    private val sessionManager by lazy { SessionManager(context) }
    private val logoutPopupManager by lazy { LogoutPopupManager(context) }


    fun showLogoutConfirmation() {
        Log.d(TAG, "showLogoutConfirmation: Displaying Logout pop-up.")

        logoutPopupManager.showLogoutPopup {
            executeUniversalLogout()
        }
    }

    /**
     * Private cleanup engine that terminates remote, cloud, and local sessions securely.
     */
    private fun executeUniversalLogout() {
        Log.i(TAG, "executeUniversalLogout: Initializing application-wide session termination framework sequence.")

        try {
            // 1. Invalidate Firebase Auth cloud access session token safely
            if (FirebaseAuth.getInstance().currentUser != null) {
                FirebaseAuth.getInstance().signOut()
                Log.d(TAG, "executeUniversalLogout: Firebase Auth cloud token invalidated successfully.")
            }

            // 2. Clear user session credentials within local SharedPreferences (KEY_IS_LOGGED_IN = false)
            sessionManager.logout()
            Log.d(TAG, "executeUniversalLogout: Local SharedPreferences session storage keys wiped.")

            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // 3. Construct clean intent clearing out activity backstack memory completely
            val intent = Intent(context, ActivityVisitorPortal::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            Log.i(TAG, "executeUniversalLogout: Activity history wiped. Redirecting safely to ActivityVisitorPortal.")

        } catch (e: Exception) {
            Log.e(TAG, "executeUniversalLogout: Critical exception caught during logout execution pipeline", e)
            Toast.makeText(context, "Error logging out: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}