package com.example.masterdashboard.login.models

import com.google.firebase.firestore.PropertyName

data class UserModelData(
    val uid: String = "",
    val fullName: String = "",
    val phone: String = "",
    val passwordHash: String = "",
    val role: String = "",
    val portalType: String = "",
    @get:PropertyName("isVerified")
    val isVerified: Boolean = false,
    val status: String = "Disabled"
)
