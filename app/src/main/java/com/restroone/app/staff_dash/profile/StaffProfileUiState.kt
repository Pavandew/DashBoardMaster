package com.restroone.app.staff_dash.profile

sealed interface StaffProfileUiState {
    object Loading : StaffProfileUiState
    data class Success(val profile: StaffProfileModel) : StaffProfileUiState
    data class Error(val message: String) : StaffProfileUiState
}