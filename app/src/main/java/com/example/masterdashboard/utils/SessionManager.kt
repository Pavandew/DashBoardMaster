package com.example.masterdashboard.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

class SessionManager(context: Context) {

    companion object {
        private const val TAG = "SessionManager"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)

    // Portal
    fun setSelectedPortal(portal: String) {
        Log.d(TAG, "setSelectedPortal: $portal")
        prefs.edit { putString(AppConstants.KEY_SELECTED_PORTAL, portal) }
    }

    fun getSelectedPortal(): String {
        val portal = prefs.getString(AppConstants.KEY_SELECTED_PORTAL, "") ?: ""
        Log.d(TAG, "getSelectedPortal: $portal")
        return portal
    }

    // Login Session
    fun setLogin(
        uid: String,
        role: String,
        phone: String,
        name: String // Added name parameter to cache user profile data
    ) {
        Log.d(TAG, "setLogin: uid=$uid, role=$role, phone=$phone, name=$name")
        prefs.edit {
            putBoolean(AppConstants.KEY_IS_LOGGED_IN, true)
                .putString(AppConstants.KEY_UID, uid)
                .putString(AppConstants.KEY_ROLE, role)
                .putString(AppConstants.KEY_PHONE, phone)
                .putString(AppConstants.KEY_NAME, name) // Save name to preferences
        }
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(AppConstants.KEY_IS_LOGGED_IN, false)
        Log.d(TAG, "isLoggedIn: $loggedIn")
        return loggedIn
    }

    fun getUid(): String {
        val uid = prefs.getString(AppConstants.KEY_UID, "") ?: ""
        Log.d(TAG, "getUid: $uid")
        return uid
    }

    fun getRole(): String {
        val role = prefs.getString(AppConstants.KEY_ROLE, "") ?: ""
        Log.d(TAG, "getRole: $role")
        return role
    }

    // Added to resolve SplashActivity log compilation mapping requirements
    fun getUserName(): String? {
        val name = prefs.getString(AppConstants.KEY_NAME, null)
        Log.d(TAG, "getUserName: $name")
        return name
    }

    // Added to resolve SplashActivity log compilation mapping requirements
    fun getPhone(): String? {
        val phone = prefs.getString(AppConstants.KEY_PHONE, null)
        Log.d(TAG, "getPhone: $phone")
        return phone
    }

    // LogOut
    fun logout() {
        Log.d(TAG, "logout: Clearing user session credentials but preserving portal theme settings")

        // Remove only user-specific data so the portal background state stays active
        prefs.edit {
            remove(AppConstants.KEY_IS_LOGGED_IN)
            remove(AppConstants.KEY_UID)
            remove(AppConstants.KEY_ROLE)
            remove(AppConstants.KEY_PHONE)
            remove(AppConstants.KEY_NAME) // Clear name on logout
        }
    }
}