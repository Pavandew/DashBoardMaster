package com.example.masterdashboard.staff_dash.waiter_screens.order.models

data class OrderExpandedItemData(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val unitPrice: Int = 0,
    val totalPrice: Int = 0,
    val orderedQuantity: Int = 0 // Existing quantity already sent to kitchen
)

data class OrderDetailExpansionUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val orderId: String = "",
    val documentId: String = "",
    val floorId: String = "",
    val tableId: String = "",
    val tableName: String = "",
    val status: ActiveOrderStatus = ActiveOrderStatus.PREPARING,
    val timeStamp: String = "",
    val items: List<OrderExpandedItemData> = emptyList(),
    val subtotal: Int = 0,
    val gstAmount: Double = 0.0,
    val grandTotal: Double = 0.0
)