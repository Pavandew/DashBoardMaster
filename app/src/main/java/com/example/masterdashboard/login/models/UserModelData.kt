package com.example.masterdashboard.login.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class UserModelData(
    @get:PropertyName("uid")
    val uid: String = "",

    @get:PropertyName("fullName")
    val fullName: String = "",
    
    // New key used in code
    @get:PropertyName("mobile")
    val mobile: String = "",
    
    // Old key used in database
    @get:PropertyName("phone")
    val phone: String = "",

    @get:PropertyName("passwordHash")
    val passwordHash: String = "",

    @get:PropertyName("role")
    val role: String = "",

    @get:PropertyName("portalType")
    val portalType: String = "",
    
    @get:PropertyName("isSetupComplete")
    val isSetupComplete: Boolean = false,

    @get:PropertyName("restaurantId")
    val restaurantId: String = "",
    
    @get:PropertyName("isVerified")
    val isVerified: Boolean = false,

    @get:PropertyName("status")
    val status: String = "Disabled"
) {
    /**
     * Helper to get the correct number regardless of which field name is used in the database.
     * Use this in your code instead of accessing 'mobile' or 'phone' directly.
     */
    fun getUnifiedMobile(): String {
        return if (mobile.isNotEmpty()) mobile else phone
    }

    /**
     * Robust check for setup completion.
     * Considers both the explicit flag and the presence of a restaurant ID.
     */
    fun isActuallySetup(): Boolean {
        return isSetupComplete || restaurantId.isNotEmpty()
    }
}
