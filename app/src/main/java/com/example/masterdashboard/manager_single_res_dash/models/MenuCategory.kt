package com.example.masterdashboard.manager_single_res_dash.models

data class MenuCategory(
    val id: String = "",
    val name: String = "",
    val itemCount: Int = 0,
    val imageResId: String = "" // Stores resource name string (e.g., "app_logo")
) {
    /**
     * Helper to convert data models safely into Map blocks for Firestore writes
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "itemCount" to itemCount,
            "imageResId" to imageResId
        )
    }
}