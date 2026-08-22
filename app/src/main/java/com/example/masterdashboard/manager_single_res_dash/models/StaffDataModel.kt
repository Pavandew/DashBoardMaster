package com.example.masterdashboard.manager_single_res_dash.models

import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class StaffDataModel(
    val id: String = "",
    val staffId: String = "",       // ✅ Added Custom ID Created by System Form
    val password: String = "",      // ✅ Added Password for Manager view & Staff Login
    val staffName: String = "",
    val mobile: String = "",
    val phone: String = "",         // ✅ Added for backward compatibility with old data
    val email: String = "",
    val gender: String = "",
    val role: String = "",
    val department: String = "",
    val joiningDate: String = "",
    val shift: String = "",
    val salary: String = "",
    val status: String = "Active",
    val permissions: List<String> = emptyList(),
    val documentType: String = "",
    val documentNumber: String = "",
): Serializable {
    /**
     * Helper to get the correct number regardless of which field name is used in the database.
     */
    fun getUnifiedMobile(): String {
        return if (mobile.isNotEmpty()) mobile else phone
    }

    fun toMap() : Map<String, Any> {
        return mapOf(
            "id" to id,
            AppConstants.FIELD_STAFF_ID to staffId,         // ✅ Synced to database payload
            AppConstants.FIELD_PASSWORD to password,       // ✅ Synced to database payload
            AppConstants.FIELD_STAFF_NAME to staffName,
            AppConstants.FIELD_MOBILE to getUnifiedMobile(), // ✅ Always save as 'mobile' but include 'phone' value if that's all we have
            AppConstants.FIELD_EMAIL to email,
            AppConstants.FIELD_GENDER to gender,
            AppConstants.FIELD_ROLE to role,
            AppConstants.FIELD_DEPARTMENT to department,
            AppConstants.FIELD_JOINING_DATE to joiningDate,
            AppConstants.FIELD_SHIFT to shift,
            AppConstants.FIELD_SALARY to salary,
            AppConstants.FIELD_STATUS to status,
            AppConstants.FIELD_PERMISSIONS to permissions,
            AppConstants.FIELD_DOCUMENT_TYPE to documentType,
            AppConstants.FIELD_DOCUMENT_NUMBER to documentNumber
        )
    }
}
