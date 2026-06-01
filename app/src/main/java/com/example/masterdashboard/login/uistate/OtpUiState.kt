package com.example.masterdashboard.login.uistate

sealed class OtpUiState {

    object Idle : OtpUiState()

    object Loading : OtpUiState()

    object CodeSent : OtpUiState()

    data class Verified(val uid: String) : OtpUiState()

    data class Error(
        val message: String
    ) : OtpUiState()
}