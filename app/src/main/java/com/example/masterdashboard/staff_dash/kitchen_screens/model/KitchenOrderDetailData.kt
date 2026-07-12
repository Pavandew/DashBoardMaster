package com.example.masterdashboard.staff_dash.kitchen_screens.model

import com.google.firebase.Timestamp
import java.io.Serializable

// FIX: Implement Serializable so the entire model instance can be sent as an argument bundle
data class KitchenOrderDetailData(
    val orderId: String = "",
    val tableName: String = "",
    val status: String = "New",
    val orderNote: String = "",
    val timestamp: Timestamp? = null,
    val items: List<OrderDetailItem> = emptyList()
) : Serializable

data class OrderDetailItem(
    val itemName: String = "",
    val quantity: Int = 1,
    val itemNote: String = ""
) : Serializable