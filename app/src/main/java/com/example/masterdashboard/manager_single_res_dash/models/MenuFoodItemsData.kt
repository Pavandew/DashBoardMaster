package com.example.masterdashboard.manager_single_res_dash.models

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class ItemVariant(
    val variantName: String = "",
    val price: Double = 0.0,
    var isSelected: Boolean = false
) : Serializable

data class MenuFoodItemsData(
    val id: String = "",
    val itemName: String = "",
    val categoryName: String = "",
    val price: String = "", // Default price if no variants
    val description: String = "",
    val status: String = "Active",
    val imageUrl: String = "",
    @get:PropertyName("isVeg")
    @set:PropertyName("isVeg")
    var isVeg: Boolean = true,
    
    val hasVariants: Boolean = false,
    val variants: List<ItemVariant> = emptyList()
) : Serializable {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "itemName" to itemName,
            "categoryName" to categoryName,
            "price" to price,
            "description" to description,
            "status" to status,
            "imageUrl" to imageUrl,
            "isVeg" to isVeg,
            "hasVariants" to hasVariants,
            "variants" to variants.map { mapOf("variantName" to it.variantName, "price" to it.price) }
        )
    }
}
