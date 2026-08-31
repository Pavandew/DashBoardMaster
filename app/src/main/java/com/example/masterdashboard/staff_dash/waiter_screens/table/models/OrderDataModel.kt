package com.example.masterdashboard.staff_dash.waiter_screens.table.models

import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.Timestamp
import java.io.Serializable

data class OrderDataModel(
    val orderId: String = "",
    val tableId: String = "",
    val floorId: String = "",
    val tableName: String = "", // Stores the human-readable table name (e.g. "T20")
    val customerName: String = "",
    val customerMobile: String = "",
    val orderType: String = AppConstants.ORDER_TYPE_DINE_IN,
    val items: List<OrderItemModel> = emptyList(),
    val specialNotes: String = "",
    val subtotal: Double = 0.0,
    val gst: Double = 0.0,
    val grandTotal: Double = 0.0,
    val orderStatus: String = AppConstants.STATUS_PENDING,
    val paymentMethod: String = "",
    val restaurantId: String = "", // Added for precise cross-collection filtering
    val waiterId: String = "", // Added to identify which waiter to notify
    val timestamp: Timestamp = Timestamp.now()
) : Serializable

data class OrderItemModel(
    val itemId: String = "",
    val itemName: String = "",
    val variantName: String = "", // Added for sizes (Small, Big, etc.)
    val price: Int = 0,
    val quantity: Int = 0,
    val rowTotal: Int = 0,
    val orderedQuantity: Int = 0, // Number of items already sent to kitchen/served
    val readyQuantity: Int = 0, // Quantity prepared by kitchen and ready for pick-up
    val itemStatus: String = AppConstants.STATUS_PENDING // PENDING, PREPARING, READY, SERVED, REJECTED
) : Serializable
