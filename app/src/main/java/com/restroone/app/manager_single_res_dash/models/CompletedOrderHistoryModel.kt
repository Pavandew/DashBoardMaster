package com.restroone.app.manager_single_res_dash.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class CompletedOrderHistoryModel(
    val orderId: String = "",
    val tableName: String = "",
    val orderType: String = "",
    val grandTotal: Double = 0.0,
    val paymentMethod: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val waiterName: String = "",
    val cashierName: String = "",
    val timestamp: Timestamp? = null,
    val docPath: String = "",
    val itemsSummary: String = "",
    val discount: Double = 0.0,
    val subtotal: Double = 0.0,
    val totalItems: Int = 0
) : Serializable
