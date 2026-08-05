package com.example.masterdashboard.staff_dash.kitchen_screens.model

import com.google.firebase.Timestamp
import java.io.Serializable

/**
 * Data model for Kitchen Orders, optimized for Firestore serialization.
 * Field names match exactly with the database schema used in Waiter and Cashier apps.
 */
data class KitchenOrderDetailData(
    var orderId: String = "",
    var tableName: String = "",
    var orderStatus: String = "PENDING",
    var specialNotes: String = "",
    var rejectionReason: String = "",
    var orderType: String = "DINE_IN",
    var restaurantId: String = "",
    var customerName: String = "",
    var customerPhone: String = "",
    var subtotal: Double = 0.0,
    var gst: Double = 0.0,
    var grandTotal: Double = 0.0,
    var paymentMethod: String = "",
    var timestamp: Timestamp? = null,
    var items: List<OrderDetailItem> = emptyList(),
    // Internal fields (not in Firestore)
    var docPath: String = "",
    var status: String = "" // Used for display mapping (New, Preparing, etc.)
) : Serializable

data class OrderDetailItem(
    var itemId: String = "",
    var itemName: String = "",
    var quantity: Int = 0,
    var orderedQuantity: Int = 0,
    var readyQuantity: Int = 0, // Number of units prepared and ready for pick-up
    var itemNote: String = "",
    var price: Int = 0,
    var rowTotal: Int = 0,
    var category: String = "Veg"
) : Serializable