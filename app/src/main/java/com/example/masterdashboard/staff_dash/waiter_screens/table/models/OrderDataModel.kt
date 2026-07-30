package com.example.masterdashboard.staff_dash.waiter_screens.table.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class OrderDataModel(
    val orderId: String = "",
    val tableName: String = "", // Stores the human-readable table name (e.g. "T20")
    val customerName: String = "",
    val customerPhone: String = "",
    val orderType: String = "NORMAL", // NORMAL or QUICK_SALE
    val items: List<OrderItemModel> = emptyList(),
    val specialNotes: String = "",
    val subtotal: Double = 0.0,
    val gst: Double = 0.0,
    val grandTotal: Double = 0.0,
    val orderStatus: String = "PENDING",
    val paymentMethod: String = "",
    val restaurantId: String = "", // Added for precise cross-collection filtering
    val timestamp: Timestamp = Timestamp.now()
) : Serializable

data class OrderItemModel(
    val itemId: String = "",
    val itemName: String = "",
    val price: Int = 0,
    val quantity: Int = 0,
    val rowTotal: Int = 0,
    val orderedQuantity: Int = 0 // Number of items already sent to kitchen/served
) : Serializable