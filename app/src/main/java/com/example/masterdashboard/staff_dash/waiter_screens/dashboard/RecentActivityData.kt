package com.example.masterdashboard.staff_dash.waiter_screens.dashboard

import java.io.Serializable

data class RecentActivityItem(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val timestampText: String = "",
    val type: String = "ORDER" // "ORDER", "BILL", "CALL"
) : Serializable

/**
 * Represents the complete UI State for the Waiter / Staff Dashboard screen.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val activeTablesCount: String = "00",
    val pendingOrdersCount: String = "00",
    val readyOrdersCount: String = "00",
    val totalServedCount: String = "00",
    val logsList: List<RecentActivityItem> = emptyList(),
    val errorMessage: String? = null
) : Serializable