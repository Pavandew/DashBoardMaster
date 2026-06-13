package com.example.masterdashboard.manager_single_res_dash.home.models

import com.google.firebase.firestore.PropertyName

data class MenuFoodItemsData(
    val id: String = "",
    val itemName: String = "",
    val categoryName: String = "",
    val price: String = "",
    val description: String = "",
    val status: String = "Active",
    val imageUrl: String = "",
    @get:PropertyName("isVeg")
    @set:PropertyName("isVeg")
    var isVeg: Boolean = true // Changed to var with PropertyName for Firestore mapping
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "itemName" to itemName,
            "categoryName" to categoryName,
            "price" to price,
            "description" to description,
            "status" to status,
            "imageUrl" to imageUrl,
            "isVeg" to isVeg // Sync to map payload
        )
    }
}