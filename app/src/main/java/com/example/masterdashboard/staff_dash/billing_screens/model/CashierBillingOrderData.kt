package com.example.masterdashboard.staff_dash.billing_screens.model

import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderItemModel
import com.google.firebase.Timestamp
import java.io.Serializable

data class CashierBillingOrderModel(
    val orderId: String = "",
    val tableId: String = "",
    val tableName: String = "Table",
    val orderType: String = "TAKE_AWAY", // e.g. "DINE_IN", "TAKE_AWAY", "DELIVERY"
    val orderStatus: String = "SERVED", // e.g. "SERVED", "BILLING_REQUESTED", "PAID"
    val itemsSummary: String = "",
    val items: List<OrderItemModel> = emptyList(),
    val subtotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val paidAt: Timestamp? = null,
    val paymentMethod: String = "",
    val docPath: String = "" // Added to facilitate updates
) : Serializable