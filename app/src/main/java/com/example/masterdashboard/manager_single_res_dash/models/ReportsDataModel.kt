package com.example.masterdashboard.manager_single_res_dash.models

data class ReportSummaryModel(
    val totalRevenue: Double = 0.0,
    val totalOrders: Int = 0,
    val avgOrderValue: Double = 0.0,
    val totalDiscounts: Double = 0.0,
    val cashAmount: Double = 0.0,
    val upiAmount: Double = 0.0,
    val cardAmount: Double = 0.0,
    val dineInSales: Double = 0.0,
    val dineInOrders: Int = 0,
    val takeawaySales: Double = 0.0,
    val takeawayOrders: Int = 0,
    val grossSubtotal: Double = 0.0,
    val totalGst: Double = 0.0
)
