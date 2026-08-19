package com.example.masterdashboard.staff_dash.kitchen_screens.model

import com.google.firebase.firestore.DocumentId

data class InventoryItem(
    @DocumentId
    val inventoryId: String = "",
    val itemName: String = "",
    val itemQuantity: Double = 0.0,
    val itemUnit: String = "",
    val minThreshold: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val inventoryCategory: String = "",
    val estimatedDaysLeft: Int = 0
) {
    fun getStockStatus(): String {
        return when {
            itemQuantity <= 0 -> "Out of Stock"
            itemQuantity <= minThreshold -> "Low Stock"
            else -> "In Stock"
        }
    }
}
