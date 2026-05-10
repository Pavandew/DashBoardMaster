package com.example.masterdashboard.signup.model

data class UserModelData(
    val fullName: String = "",
    val phone: String = "",
    val password: String = "",
    val isVerified: Boolean = false,
    val status: String = "Disabled",
)
