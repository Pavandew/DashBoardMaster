package com.example.masterdashboard.manager_single_res_dash.home.uistate

sealed interface FirebaseUiState {
    object Idle : FirebaseUiState
    object Loading : FirebaseUiState
    object Success : FirebaseUiState
    data class Error(val message: String) : FirebaseUiState
}