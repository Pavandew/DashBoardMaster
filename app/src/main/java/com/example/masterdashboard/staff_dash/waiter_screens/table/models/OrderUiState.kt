package com.example.masterdashboard.staff_dash.waiter_screens.table.models

data class OrderUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<MenuCategoryData> = emptyList(),
    val menuItems: List<FoodItemData> = emptyList(),     // Displays the FILTERED items on the menu screen
    val allMenuItems: List<FoodItemData> = emptyList(),  // FIX: Absolute master list tracking ALL quantities
    val cartSummary: CartSummaryState = CartSummaryState(0, 0)
)
