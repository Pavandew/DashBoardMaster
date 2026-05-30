package com.example.masterdashboard.login.uistate

sealed class SignUpUiState {

    object  Idle: SignUpUiState()

    object  Loading: SignUpUiState()

    data class  Success(
        val message: String,
        val phone: String,
        val role: String,
        val portalType: String
    ): SignUpUiState()

    data class Error(val field: String, val message: String): SignUpUiState()

}