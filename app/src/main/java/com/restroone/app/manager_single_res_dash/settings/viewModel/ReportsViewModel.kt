package com.restroone.app.manager_single_res_dash.settings.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restroone.app.manager_single_res_dash.models.CompletedOrderHistoryModel
import com.restroone.app.manager_single_res_dash.models.ReportSummaryModel
import com.restroone.app.manager_single_res_dash.settings.repo.ReportsRepository
import com.restroone.app.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    companion object {
        private const val TAG = "ReportsViewModel"
    }

    private val repository = ReportsRepository()

    private val _reportState = MutableStateFlow<ResourceUiState<ReportSummaryModel>>(ResourceUiState.Loading)
    val reportState: StateFlow<ResourceUiState<ReportSummaryModel>> = _reportState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ReportsRepository.TimeFilter.TODAY)
    val selectedFilter: StateFlow<ReportsRepository.TimeFilter> = _selectedFilter.asStateFlow()

    private var masterCompletedOrders: List<CompletedOrderHistoryModel> = emptyList()

    private val _filteredOrders = MutableStateFlow<List<CompletedOrderHistoryModel>>(emptyList())
    val filteredOrders: StateFlow<List<CompletedOrderHistoryModel>> = _filteredOrders.asStateFlow()

    private var currentQuery: String = ""

    private var reportJob: Job? = null

    fun filterOrders(query: String) {
        currentQuery = query.trim().lowercase()
        if (currentQuery.isEmpty()) {
            _filteredOrders.value = masterCompletedOrders
        } else {
            _filteredOrders.value = masterCompletedOrders.filter { order ->
                order.orderId.lowercase().contains(currentQuery) ||
                order.tableName.lowercase().contains(currentQuery) ||
                order.customerPhone.contains(currentQuery) ||
                order.customerName.lowercase().contains(currentQuery) ||
                order.waiterName.lowercase().contains(currentQuery) ||
                order.cashierName.lowercase().contains(currentQuery) ||
                order.paymentMethod.lowercase().contains(currentQuery)
            }
        }
    }

    fun loadReportData(
        managerId: String,
        filter: ReportsRepository.TimeFilter = _selectedFilter.value,
        startDateStr: String? = null,
        endDateStr: String? = null
    ) {
        Log.i(TAG, "loadReportData called: managerId='$managerId', filter=$filter, startDate='$startDateStr', endDate='$endDateStr'")
        _selectedFilter.value = filter
        _reportState.value = ResourceUiState.Loading

        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            repository.getReportSummaryStream(managerId, filter, startDateStr, endDateStr)
                .catch { exception ->
                    Log.e(TAG, "Error collecting report summary stream", exception)
                    _reportState.value = ResourceUiState.Error(exception.message ?: "Failed to load report")
                }
                .collect { summary ->
                    Log.d(TAG, "New Report Summary received: Revenue=₹${summary.totalRevenue}, Orders=${summary.totalOrders}")
                    masterCompletedOrders = summary.completedOrders
                    filterOrders(currentQuery)
                    _reportState.value = ResourceUiState.Success(summary)
                }
        }
    }
}
