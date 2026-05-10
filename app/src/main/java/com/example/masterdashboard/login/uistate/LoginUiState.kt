package com.example.masterdashboard.login.uistate

sealed class LoginUiState{

    object Idle: LoginUiState()

    object  Loading: LoginUiState()

    data class Success(val message: String): LoginUiState()

    data class Error(
        val field: String = "",
        val message: String
    ) : LoginUiState()
}