package com.example.masterdashboard.staff_dash.waiter_screens.table.models

import java.io.Serializable

data class AddonItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    var isSelected: Boolean = false
) : Serializable

data class MenuItemDetailData(
    val itemId: String = "",
    val itemName: String = "Paneer Tikka",
    val description: String = "Tender cottage cheese cubes marinated in spices and grilled to perfection in a tandoor.",
    val basePrice: Double = 240.0,
    val rating: String = "4.5",
    val imageUrl: String = "",
    val isVeg: Boolean = true,
    val availableAddons: List<AddonItem> = listOf(
        AddonItem("1", "Extra Butter", 30.0),
        AddonItem("2", "Green Chutney", 20.0),
        AddonItem("3", "Extra Masala", 15.0)
    )
) : Serializable
