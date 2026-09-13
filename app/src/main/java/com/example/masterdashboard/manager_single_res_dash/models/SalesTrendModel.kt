package com.example.masterdashboard.manager_single_res_dash.models

data class ShiftSales(
    val morningSales: Double = 0.0,
    val morningOrders: Int = 0,
    val lunchSales: Double = 0.0,
    val lunchOrders: Int = 0,
    val eveningSales: Double = 0.0,
    val eveningOrders: Int = 0,
    val dinnerSales: Double = 0.0,
    val dinnerOrders: Int = 0,
    val peakShiftName: String = "No Rush"
)
