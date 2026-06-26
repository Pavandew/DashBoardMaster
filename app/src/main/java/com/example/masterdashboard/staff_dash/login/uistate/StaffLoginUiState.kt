package com.example.masterdashboard.staff_dash.login.uistate


sealed interface StaffLoginUiState {
    object Idle : StaffLoginUiState
    object Loading : StaffLoginUiState
    data class Success(
        val staffName: String,
        val restaurantOwnerUid: String,
        val staffDocId: String,
        val staffId: String,
        val role: String,
        val permissions: List<String>
    ) : StaffLoginUiState
    data class ValidationError(val message: String) : StaffLoginUiState
    data class AuthError(val message: String) : StaffLoginUiState
}