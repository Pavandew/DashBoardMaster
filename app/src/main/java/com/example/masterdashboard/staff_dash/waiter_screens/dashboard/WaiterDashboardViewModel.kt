package com.example.masterdashboard.staff_dash.waiter_screens.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WaiterDashboardViewModel(
    private val repository: WaiterDashboardRepository = WaiterDashboardRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "StaffDashboardViewModel"
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun observeRealtimeDashboard(managerId: String) {
        viewModelScope.launch {
            Log.i(TAG, "Connecting real-time Firestore dashboard channels for Manager: $managerId")

            val metricsFlow = repository.streamDashboardMetrics(managerId)
            val activitiesFlow = repository.streamRecentActivities(managerId, limit = 5)

            combine(metricsFlow, activitiesFlow) { metrics, activities ->
                DashboardUiState(
                    activeTablesCount = metrics.activeTables,
                    pendingOrdersCount = metrics.pendingOrders,
                    readyOrdersCount = metrics.readyOrders,
                    totalServedCount = metrics.totalServed,
                    logsList = activities
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    class WaiterDashboardViewModelFactory(
        private val repository: WaiterDashboardRepository = WaiterDashboardRepository()
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WaiterDashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WaiterDashboardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}