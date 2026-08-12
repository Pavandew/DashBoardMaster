package com.example.masterdashboard.login.uistate

sealed class LoginUiState{

    object Idle: LoginUiState()

    object  Loading: LoginUiState()

    data class Success(
        val message: String,
        val uid: String,
        val role: String,
        val portalType: String,
        val isRestaurantSetup: Boolean = false,
        val restaurantId: String = "",
        val fullName: String = ""
    ): LoginUiState()

    data class Error(
        val field: String = "",
        val message: String
    ) : LoginUiState()
}