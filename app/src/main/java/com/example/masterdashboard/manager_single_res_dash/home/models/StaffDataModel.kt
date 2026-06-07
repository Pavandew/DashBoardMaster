package com.example.masterdashboard.manager_single_res_dash.home.models

data class StaffDataModel(
    val id: String = "",
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
) {
    fun toMap() : Map<String, Any> {
        return mapOf(
            "id" to id,
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
