package com.restroone.app.staff_dash.waiter_screens.order.models

enum class ActiveOrderStatus {
    PENDING, PREPARING, READY, SERVED, BILLING, PAID
}

// UI Presentation model for individual active order cards
data class ActiveOrderCardData(
    val orderId: String = "",
    val tableName: String = "",
    val totalItems: Int = 0,
    val orderTime: String = "",
    val status: ActiveOrderStatus = ActiveOrderStatus.PREPARING,
    val docPath: String = ""
)

// Data representation for your reusable top filter chips
data class OrderStatusFilterData(
    val id: String,
    val name: String,
    val statusType: ActiveOrderStatus?,
    val isSelected: Boolean = false
)

// Main screen UI wrapper keeping stream handling clean and safe
data class ActiveOrdersUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val filters: List<OrderStatusFilterData> = emptyList(),
    val visibleOrders: List<ActiveOrderCardData> = emptyList()
)
