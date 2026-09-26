package com.example.masterdashboard.manager_single_res_dash.settings.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.models.ReportSummaryModel
import com.example.masterdashboard.manager_single_res_dash.settings.repo.ReportsRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
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

    private var reportJob: Job? = null

    fun loadReportData(
        managerId: String,
        filter: ReportsRepository.TimeFilter = _selectedFilter.value,
        customDateStr: String? = null
    ) {
        Log.i(TAG, "loadReportData called: managerId='$managerId', filter=$filter, customDate='$customDateStr'")
        _selectedFilter.value = filter
        _reportState.value = ResourceUiState.Loading

        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            repository.getReportSummaryStream(managerId, filter, customDateStr)
                .catch { exception ->
                    Log.e(TAG, "Error collecting report summary stream", exception)
                    _reportState.value = ResourceUiState.Error(exception.message ?: "Failed to load report")
                }
                .collect { summary ->
                    Log.d(TAG, "New Report Summary received: Revenue=₹${summary.totalRevenue}, Orders=${summary.totalOrders}")
                    _reportState.value = ResourceUiState.Success(summary)
                }
        }
    }
}
