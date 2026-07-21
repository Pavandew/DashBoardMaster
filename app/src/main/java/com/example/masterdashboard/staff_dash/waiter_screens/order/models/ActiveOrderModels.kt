package com.example.masterdashboard.staff_dash.waiter_screens.order.models

enum class ActiveOrderStatus {
    PREPARING, READY, SERVED
}

// Data representation for individual active order cards
data class ActiveOrderCardData(
    val orderId: String,
    val tableName: String,
    val totalItems: Int,
    val orderTime: String,
    val status: ActiveOrderStatus
)

// Data representation for your reusable top filter chips
data class OrderStatusFilterData(
    val id: String,
    val name: String, // e.g., "All (5)", "Preparing (3)"
    val statusType: ActiveOrderStatus?, // null represents the "All" category filter
    val isSelected: Boolean = false
)

// Main screen UI wrapper keeping stream handling clean and safe
data class ActiveOrdersUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val filters: List<OrderStatusFilterData> = emptyList(),
    val visibleOrders: List<ActiveOrderCardData> = emptyList()
)