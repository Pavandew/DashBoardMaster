package com.example.masterdashboard.staff_dash.home.table.models


// Model for horizontal categories
data class MenuCategoryData(
    val id: String,
    val name: String,
    val isSelected: Boolean = false
)

// Model for food items in the vertical list
data class FoodItemData(
    val id: String,
    val name: String,
    val price: Int,
    val imageUrl: String,
    val currentQuantity: Int = 0  // track active item stepper additions locally
)

// Wrapper object summarizing the baseline checkout state
data class CartSummaryState(
    val totalItems: Int,
    val totalPrice: Int,
)