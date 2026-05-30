package com.example.masterdashboard.staff_dash.home.order.views.models

// individual item row matching your design sheet
data class OrderExpandedItemData(
    val id: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Int,
    val totalPrice: Int
)

// main screen UI state class wrapper
data class OrderDetailExpansionUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val orderId: String = "",
    val tableId: String = "",
    val status: ActiveOrderStatus = ActiveOrderStatus.PREPARING,
    val timeStamp: String = "",
    val items: List<OrderExpandedItemData> = emptyList(),
    val subtotal: Int = 0,
    val gstAmount: Double = 0.0,
    val grandTotal: Double = 0.0
)
