package com.example.masterdashboard.staff_dash.kitchen_screens.model

import com.google.firebase.Timestamp


data class KitchenOrderData(
    val orderId: String = "",
    val itemsSummary: String = "",
    val tableName: String = "",
    val status: String = "New", // New, Preparing, Ready, Completed
    val timestamp: Timestamp? = null
)
