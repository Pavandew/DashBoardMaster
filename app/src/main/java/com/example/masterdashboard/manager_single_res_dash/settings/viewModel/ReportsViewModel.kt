package com.example.masterdashboard.manager_single_res_dash.settings.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.models.ReportSummaryModel
import com.example.masterdashboard.manager_single_res_dash.settings.repo.ReportsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {

    companion object {
        private const val TAG = "ReportsViewModel"
    }

    private val repository = ReportsRepository()

    private val _reportState = MutableStateFlow(ReportSummaryModel())
    val reportState: StateFlow<ReportSummaryModel> = _reportState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ReportsRepository.TimeFilter.TODAY)
    val selectedFilter: StateFlow<ReportsRepository.TimeFilter> = _selectedFilter.asStateFlow()

    private var reportJob: Job? = null

    fun loadReportData(managerId: String, filter: ReportsRepository.TimeFilter = _selectedFilter.value) {
        Log.i(TAG, "loadReportData called: managerId='$managerId', filter=$filter")
        _selectedFilter.value = filter
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            repository.getReportSummaryStream(managerId, filter).collect { summary ->
                Log.d(TAG, "New Report Summary received from Repository: Revenue=₹${summary.totalRevenue}, Orders=${summary.totalOrders}")
                _reportState.value = summary
            }
        }
    }
}
