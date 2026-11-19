package com.restroone.app.manager_single_res_dash.models

import java.io.Serializable

data class MenuCategory(
    val menuCategoryId: String = "",
    val menuCategoryName: String = "",
    val itemCount: Int = 0,
    val imageResId: String = "",
    val categoryImageUrl: String = ""
) : Serializable {
    /**
     * Helper to convert data models safely into Map blocks for Firestore writes
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "menuCategoryId" to menuCategoryId,
            "menuCategoryName" to menuCategoryName,
            "itemCount" to itemCount,
            "imageResId" to imageResId,
            "categoryImageUrl" to categoryImageUrl
        )
    }
}