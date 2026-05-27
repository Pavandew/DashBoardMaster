package com.example.masterdashboard.staff_dash.home.table.models

data class OrderUiState(
    val categories: List<MenuCategoryData> = emptyList(),
    val menuItems: List<FoodItemData> = emptyList(),
    val cartSummary: CartSummaryState = CartSummaryState(0, 0),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
