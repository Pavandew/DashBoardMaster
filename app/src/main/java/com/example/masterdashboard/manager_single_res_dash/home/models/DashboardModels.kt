package com.example.masterdashboard.manager_single_res_dash.home.models

// Data class for Top Selling food items
data class TopSellingFoodItem(
    val id: String,
    val name: String,
    val orderCount: Int,
    val totalPriceText: String,
    val imageResId: Int
)

// Data class to easily package metrics for the Overview Grid cards
data class StatMetric(
    val title: String,
    val value: String,
    val trend: String,
    val isPositiveTrend: Boolean
)

// Data class to package Order Summary counts
data class DashboardSummary(
    val newCount: String = "0",
    val kitchenCount: String = "0",
    val readyCount: String = "0",
    val servedCount: String = "0",
    val cancelledCount: String = "0"
)