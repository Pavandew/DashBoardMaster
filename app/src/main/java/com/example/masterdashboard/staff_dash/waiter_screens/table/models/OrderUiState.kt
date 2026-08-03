package com.example.masterdashboard.staff_dash.waiter_screens.table.models

data class OrderUiState(
    val isLoading: Boolean = true, // Default to true so loader shows immediately
    val errorMessage: String? = null,
    val categories: List<MenuCategoryData> = emptyList(),
    val dietFilters: List<MenuCategoryData> = emptyList(), // Reusing MenuCategoryData for Diet Chips
    val activeFilterId: String = "ALL_ITEMS",           // Controls which items are FILTERED (All vs Specific)
    val activeDietFilter: String = "ALL",               // "ALL", "VEG", "NON-VEG"
    val menuItems: List<FoodItemData> = emptyList(),     // Displays the FILTERED items on the menu screen
    val displayItems: List<MenuItemType> = emptyList(),  // List with headers for the UI
    val allMenuItems: List<FoodItemData> = emptyList(),  // FIX: Absolute master list tracking ALL quantities
    val cartSummary: CartSummaryState = CartSummaryState(0, 0)
)
