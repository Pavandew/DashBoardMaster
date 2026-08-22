package com.example.masterdashboard.staff_dash.waiter_screens.table.models

import com.example.masterdashboard.manager_single_res_dash.models.ItemVariant
import com.example.masterdashboard.utils.AppConstants

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
 */
data class FoodItemData(
    val id: String,
    val name: String,
    val price: Int,
    val imageUrl: String,
    val categoryId: String = "",   // FIX: Added to support seamless sub-collection item filtering
    val categoryName: String = "", // Added to display category name in tag
    val isVeg: Boolean = true,     // Added to support Diet Filtering (Veg/Non-Veg)
    var variantName: String = "",  // Added: e.g. "Small", "Regular"
    var selectedAddons: List<String> = emptyList(), // Added: List of selected addon names
    var currentQuantity: Int = 0,   // Tracks active item stepper additions locally
    var previousQuantity: Int = 0,  // Tracks items already saved in the active order
    var itemStatus: String = AppConstants.STATUS_PENDING, // Tracks if item was PENDING, READY, SERVED, etc.
    val hasVariants: Boolean = false, // Added to know if we should show customization sheet
    val variantsList: List<ItemVariant> = emptyList(), // Added to store available variants
    var availableAddons: List<AddonItem> = emptyList() // NEW: Fetched from Firebase sub-collection
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
