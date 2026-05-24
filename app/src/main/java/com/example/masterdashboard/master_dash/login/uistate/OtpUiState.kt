package com.example.masterdashboard.master_dash.login.uistate

sealed class OtpUiState {

    object Idle : OtpUiState()

    object Loading : OtpUiState()

    object CodeSent : OtpUiState()

    object Verified : OtpUiState()

    data class Error(
        val message: String
    ) : OtpUiState()
}