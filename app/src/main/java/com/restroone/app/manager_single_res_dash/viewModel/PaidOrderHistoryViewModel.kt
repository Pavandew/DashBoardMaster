package com.restroone.app.manager_single_res_dash.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restroone.app.manager_single_res_dash.models.CompletedOrderHistoryModel
import com.restroone.app.manager_single_res_dash.settings.repo.ReportsRepository
import com.restroone.app.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PaidOrderHistoryViewModel : ViewModel() {

    companion object {
        private const val TAG = "PaidOrderHistoryVM"
    }

    private val repository = ReportsRepository()

    private val _uiState = MutableStateFlow<ResourceUiState<List<CompletedOrderHistoryModel>>>(ResourceUiState.Loading)
    val uiState: StateFlow<ResourceUiState<List<CompletedOrderHistoryModel>>> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ReportsRepository.TimeFilter.TODAY)
    val selectedFilter: StateFlow<ReportsRepository.TimeFilter> = _selectedFilter.asStateFlow()

    private var masterCompletedOrders: List<CompletedOrderHistoryModel> = emptyList()
    private var currentQuery: String = ""
    private var job: Job? = null

    fun loadPaidOrders(
        managerId: String,
        filter: ReportsRepository.TimeFilter = _selectedFilter.value,
        startDateStr: String? = null,
        endDateStr: String? = null
    ) {
        Log.i(TAG, "loadPaidOrders called: managerId='$managerId', filter=$filter, startDate='$startDateStr', endDate='$endDateStr'")
        _selectedFilter.value = filter
        _uiState.value = ResourceUiState.Loading

        job?.cancel()
        job = viewModelScope.launch {
            repository.getReportSummaryStream(managerId, filter, startDateStr, endDateStr)
                .catch { exception ->
                    Log.e(TAG, "Error fetching paid orders", exception)
                    _uiState.value = ResourceUiState.Error(exception.message ?: "Failed to load paid orders")
                }
                .collect { summary ->
                    Log.d(TAG, "Received ${summary.completedOrders.size} paid orders from repository.")
                    masterCompletedOrders = summary.completedOrders
                    applySearchFilter()
                }
        }
    }

    fun filterOrders(query: String) {
        currentQuery = query.trim().lowercase()
        applySearchFilter()
    }

    private fun applySearchFilter() {
        if (currentQuery.isEmpty()) {
            _uiState.value = ResourceUiState.Success(masterCompletedOrders)
        } else {
            val filtered = masterCompletedOrders.filter { order ->
                order.orderId.lowercase().contains(currentQuery) ||
                order.tableName.lowercase().contains(currentQuery) ||
                order.customerPhone.contains(currentQuery) ||
                order.customerName.lowercase().contains(currentQuery) ||
                order.waiterName.lowercase().contains(currentQuery) ||
                order.cashierName.lowercase().contains(currentQuery) ||
                order.paymentMethod.lowercase().contains(currentQuery)
            }
            _uiState.value = ResourceUiState.Success(filtered)
        }
    }
}
