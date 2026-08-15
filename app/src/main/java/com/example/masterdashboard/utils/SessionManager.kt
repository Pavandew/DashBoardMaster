package com.example.masterdashboard.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel
import com.google.gson.Gson
import org.json.JSONArray

class SessionManager(context: Context) {

    companion object {
        private const val TAG = "SessionManager"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)

    // Portal Section Control Layer
    fun setSelectedPortal(portal: String) {
        Log.d(TAG, "setSelectedPortal: $portal")
        prefs.edit { putString(AppConstants.KEY_SELECTED_PORTAL, portal) }
    }

    fun getSelectedPortal(): String {
        val portal = prefs.getString(AppConstants.KEY_SELECTED_PORTAL, "") ?: ""
        Log.d(TAG, "getSelectedPortal: $portal")
        return portal
    }

    // MAIN LOGIN SESSION AUTHORIZATION CONTROLS
    /**
     * Set Login session states.
     * By adding `= ""` to staffId, Kotlin makes it OPTIONAL.
     * Manager and Master screens can keep calling 4 parameters without breaking!
     */
    fun setLogin(
        uid: String,
        role: String,
        mobile: String,
        name: String,
        staffId: String = "",
        staffDocId: String = ""
    ) {
        Log.d(TAG, "setLogin: uid=$uid, role=$role, mobile=$mobile, name=$name, staffId=$staffId, staffDocId=$staffDocId")
        prefs.edit {
            putBoolean(AppConstants.KEY_IS_LOGGED_IN, true)
                .putString(AppConstants.KEY_UID, uid)
                .putString(AppConstants.KEY_ROLE, role)
                .putString(AppConstants.KEY_MOBILE, mobile)
                .putString(AppConstants.KEY_NAME, name)

            if (staffId.isNotEmpty()) {
                putString(AppConstants.KEY_STAFF_ID, staffId)
            }
            if (staffDocId.isNotEmpty()) {
                putString(AppConstants.KEY_STAFF_DOC_ID, staffDocId)
            }
        }
    }
    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(AppConstants.KEY_IS_LOGGED_IN, false)
        Log.d(TAG, "isLoggedIn: $loggedIn")
        return loggedIn
    }

    // 🛡️ REUSABLE EXPLICIT FIELD CACHE METHODS
    fun saveUid(uid: String) {
        Log.d(TAG, "saveUid: $uid")
        prefs.edit { putString(AppConstants.KEY_UID, uid) }
    }

    fun getUid(): String {
        val uid = prefs.getString(AppConstants.KEY_UID, "") ?: ""
        Log.d(TAG, "getUid: $uid")
        return uid
    }

    fun saveRole(role: String) {
        Log.d(TAG, "saveRole: $role")
        prefs.edit { putString(AppConstants.KEY_ROLE, role) }
    }

    fun getRole(): String {
        val role = prefs.getString(AppConstants.KEY_ROLE, "") ?: ""
        Log.d(TAG, "getRole: $role")
        return role
    }

    // Caches custom custom-generated alphanumeric Staff ID values (e.g. PAVAN9730)
    fun saveStaffId(staffId: String) {
        Log.d(TAG, "saveStaffId: $staffId")
        prefs.edit { putString(AppConstants.KEY_STAFF_ID, staffId) }
    }

    //  Pulls cached Staff ID mapping profiles
    fun getStaffId(): String {
        val staffId = prefs.getString(AppConstants.KEY_STAFF_ID, "") ?: ""
        Log.d(TAG, "getStaffId: $staffId")
        return staffId
    }

    fun saveStaffDocId(staffDocId: String) {
        Log.d(TAG, "saveStaffDocId: $staffDocId")
        prefs.edit { putString(AppConstants.KEY_STAFF_DOC_ID, staffDocId) }
    }

    fun getStaffDocId(): String {
        val id = prefs.getString(AppConstants.KEY_STAFF_DOC_ID, "") ?: ""
        Log.d(TAG, "getStaffDocId: $id")
        return id
    }

    // Added to resolve SplashActivity log compilation mapping requirements
    fun getUserName(): String? {
        val name = prefs.getString(AppConstants.KEY_NAME, null)
        Log.d(TAG, "getUserName: $name")
        return name
    }

    // Added to resolve SplashActivity log compilation mapping requirements
    fun getPhone(): String? {
        val mobile = prefs.getString(AppConstants.KEY_MOBILE, null)
        Log.d(TAG, "getMobile: $mobile")
        return mobile
    }

    fun saveRestaurantId(restaurantId: String) {
        Log.d(TAG, "saveRestaurantId: $restaurantId")
        prefs.edit { putString(AppConstants.KEY_RESTAURANT_ID, restaurantId) }
    }

    fun getRestaurantId(): String {
        val id = prefs.getString(AppConstants.KEY_RESTAURANT_ID, "") ?: ""
        Log.d(TAG, "getRestaurantId: $id")
        return id
    }

    // 🏗️ RESTAURANT SETUP CONTROL
    fun setRestaurantSetup(isSetup: Boolean) {
        Log.i(TAG, "setRestaurantSetup: Updating status to $isSetup")
        prefs.edit { putBoolean(AppConstants.KEY_IS_RESTAURANT_SETUP, isSetup) }
    }

    fun isRestaurantSetup(): Boolean {
        val isSetup = prefs.getBoolean(AppConstants.KEY_IS_RESTAURANT_SETUP, false)
        Log.d(TAG, "isRestaurantSetup: $isSetup")
        return isSetup
    }

    // 📝 REGISTRATION DRAFT PERSISTENCE
    fun saveRegistrationDraft(data: RegistrationDataModel) {
        val json = Gson().toJson(data)
        Log.d(TAG, "saveRegistrationDraft: Data cached locally")
        prefs.edit { putString(AppConstants.KEY_REGISTRATION_DRAFT, json) }
    }

    fun getRegistrationDraft(): RegistrationDataModel? {
        val json = prefs.getString(AppConstants.KEY_REGISTRATION_DRAFT, null)
        return if (json != null) {
            Log.d(TAG, "getRegistrationDraft: Draft found and restored")
            Gson().fromJson(json, RegistrationDataModel::class.java)
        } else {
            Log.d(TAG, "getRegistrationDraft: No local draft found")
            null
        }
    }

    fun clearRegistrationDraft() {
        Log.d(TAG, "clearRegistrationDraft: Local cache cleared")
        prefs.edit { remove(AppConstants.KEY_REGISTRATION_DRAFT) }
    }

    // 🛡️ NEW: DYNAMIC PERMISSIONS ARRAY LIST CACHE CONTROL LOGIC
    /**
     * Serializes employee custom permissions string arrays into JSON formatting
     * and stores them directly into localized SharedPreferences strings values.
     */
    fun savePermissions(permission: List<String>) {
        Log.d(TAG, "savePermissions: Serializing access matrix: $permission")
        val jsonArray = JSONArray(permission)
        prefs.edit{ putString(AppConstants.KEY_PERMISSIONS, jsonArray.toString()) }
    }
    /**
     * Pulls the stored JSON authorization keys string array container, unpacks it,
     * and returns a standard type-safe Kotlin String List.
     */
    fun getPermissions(): List<String> {
        val jsonStr = prefs.getString(AppConstants.KEY_PERMISSIONS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            Log.d(TAG, "getPermissions: Unpacked active authorization list keys: $list")
            list
        } catch (e: Exception) {
            Log.e(TAG, "getPermissions: Structural evaluation exception caught parsing json permission string", e)
            emptyList()
        }
    }

    /**
     * Direct one-line utility validation helper checker.
     * Pass an expected access string key (e.g. "menu_access") to verify permissions quickly.
     */
    fun hasPermission(permissionKey: String): Boolean {
        val currentPermissions = getPermissions()
        val hasAccess = currentPermissions.contains(permissionKey)
        Log.d(TAG, "🔒 Rule Check: Verifying target boundary access mapping context [Key: $permissionKey | Result: $hasAccess]")
        return hasAccess
    }

    // 🗑️ SESSION TERMINATION CONTROL UTILITIES
    fun logout() {
        Log.d(TAG, "logout: Clearing user session credentials but preserving portal theme settings")

        // Remove only user-specific data so the portal background state stays active
        prefs.edit {
            remove(AppConstants.KEY_IS_LOGGED_IN)
            remove(AppConstants.KEY_UID)
            remove(AppConstants.KEY_ROLE)
            remove(AppConstants.KEY_MOBILE)
            remove(AppConstants.KEY_NAME)
            remove(AppConstants.KEY_IS_RESTAURANT_SETUP)
            remove(AppConstants.KEY_RESTAURANT_ID)
        }
    }
}
