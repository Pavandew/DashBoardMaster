package com.example.masterdashboard.manager_single_res_dash.form_screen.uiState

sealed class RegistrationUiState {
    object Idle : RegistrationUiState()
    object Loading : RegistrationUiState()
    data class Success(val message: String, val restaurantId: String) : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}
