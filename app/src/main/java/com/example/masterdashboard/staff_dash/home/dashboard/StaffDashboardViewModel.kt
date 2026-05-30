package com.example.masterdashboard.staff_dash.home.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeTablesCount: String = "0",
    val pendingOrdersCount: String = "0",
    val readyOrdersCount: String = "0",
    val totalServedCount: String = "0",
    val logsList: List<RecentActivityItem> = emptyList()
)

class StaffDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardMetrics() {
        viewModelScope.launch {
            // Mock Data Stream. Pipe your live active Firestore flow snapshot listeners here!
            val mockedActivities = listOf(
                RecentActivityItem("1", "T-05", "New order placed", "2 min ago", "ORDER"),
                RecentActivityItem("2", "T-02", "Bill Requested", "5 min ago", "BILL"),
                RecentActivityItem("3", "T-07", "Call Waiter", "12 min ago", "CALL"),
                RecentActivityItem("4", "T-11", "Extra Spoon requested", "15 min ago", "CALL"),
                RecentActivityItem("5", "T-01", "Order Delivered Successfully", "20 min ago", "ORDER"),
                RecentActivityItem("6", "T-09", "This should be hidden due to limit rules", "1 hr ago", "ORDER")
            )

            _uiState.value = DashboardUiState(
                activeTablesCount = "12",
                pendingOrdersCount = "05",
                readyOrdersCount = "03",
                totalServedCount = "48",
                logsList = mockedActivities
            )
        }
    }

    class StaffDashboardViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StaffDashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StaffDashboardViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel layout configuration error")
        }
    }
}