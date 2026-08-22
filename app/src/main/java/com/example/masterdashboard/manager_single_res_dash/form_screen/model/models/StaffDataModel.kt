package com.example.masterdashboard.manager_single_res_dash.models

import java.io.Serializable

data class StaffDataModel(
    val id: String = "",
    val staffId: String = "",       // ✅ Added Custom ID Created by System Form
    val password: String = "",      // ✅ Added Password for Manager view & Staff Login
    val staffName: String = "",
    val mobile: String = "",
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
    fun toMap() : Map<String, Any> {
        return mapOf(
            "id" to id,
            "staffId" to staffId,         // ✅ Synced to database payload
            "password" to password,       // ✅ Synced to database payload
            "staffName" to staffName,
            "mobile" to mobile,
            "email" to email,
            "gender" to gender,
            "role" to role,
            "department" to department,
            "joiningDate" to joiningDate,
            "shift" to shift,
            "salary" to salary,
            "status" to status,
            "permissions" to permissions,
            "documentType" to documentType,
            "documentNumber" to documentNumber
        )
    }
}