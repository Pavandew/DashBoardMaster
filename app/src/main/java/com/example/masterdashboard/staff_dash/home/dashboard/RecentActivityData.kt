package com.example.masterdashboard.staff_dash.home.dashboard


data class RecentActivityItem(
    val id: String,
    val tableId: String,
    val description: String, // e.g., "New order placed", "Call Waiter"
    val timeAgo: String,     // e.g., "2 min ago"
    val alertType: String    // e.g., "ORDER", "BILL", "CALL" to handle dynamic icons
)
