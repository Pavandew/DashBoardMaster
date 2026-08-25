package com.example.masterdashboard.login.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class UserModelData(
    val uid: String = "",
    val fullName: String = "",
    
    // New key used in code
    val mobile: String = "",
    
    // Old key used in database - Firestore will map 'phone' to this property if it exists
    val phone: String = "",

    val passwordHash: String = "",
    val role: String = "",
    val portalType: String = "",
    
    val isSetupComplete: Boolean = false,
    val restaurantId: String = "",
    
    @get:PropertyName("isVerified")
    val isVerified: Boolean = false,
    val status: String = "Disabled"
) {
    /**
     * Helper to get the correct number regardless of which field name is used in the database.
     * Use this in your code instead of accessing 'mobile' or 'phone' directly.
     */
    fun getUnifiedMobile(): String {
        return if (mobile.isNotEmpty()) mobile else phone
    }
}
