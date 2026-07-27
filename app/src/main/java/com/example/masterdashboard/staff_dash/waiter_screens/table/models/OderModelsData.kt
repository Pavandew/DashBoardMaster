package com.example.masterdashboard.staff_dash.waiter_screens.table.models

import android.util.Log

/**
 * Model for horizontal course category chips (e.g., Starters, Main Course, Desserts).
 */
data class MenuCategoryData(
    val id: String,
    val name: String,
    val isSelected: Boolean = false
)

/**
 * Model for food items in the vertical menu list.
 *
 * NOTE: Added 'categoryId' so the ViewModel can map dishes to their parent categories.
 */
data class FoodItemData(
    val id: String,
    val name: String,
    val price: Int,
    val imageUrl: String,
    val categoryId: String = "",   // FIX: Added to support seamless sub-collection item filtering
    val categoryName: String = "", // Added to display category name in tag
    val isVeg: Boolean = true,     // Added to support Diet Filtering (Veg/Non-Veg)
    val currentQuantity: Int = 0,   // Tracks active item stepper additions locally
    val previousQuantity: Int = 0  // Tracks items already saved in the active order
)

/**
 * Wrapper for the menu list to support headers and items
 */
sealed class MenuItemType {
    data class Header(val id: String, val name: String) : MenuItemType()
    data class Food(val food: FoodItemData) : MenuItemType()
}

/**
 * Wrapper state summarizing the baseline checkout configurations.
 */
data class CartSummaryState(
    val totalItems: Int,
    val totalPrice: Int
)

/**
 * Unified UI container managing progress indicators, error payloads, and menu item updates.
 */
