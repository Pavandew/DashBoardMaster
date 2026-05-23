package com.example.masterdashboard.master_dash.login.models

data class UserModelData(
    val fullName: String = "",
    val phone: String = "",
    val password: String = "",
    val isVerified: Boolean = false,
    val status: String = "Disabled",
)