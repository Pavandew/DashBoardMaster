package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.WaiterTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WaiterTableViewModel(
    private val repository: WaiterTableRepository
) : ViewModel() {

    class TableViewModelFactory(private val repository: WaiterTableRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WaiterTableViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WaiterTableViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val TAG = "Table_Flow_Debug"
    }

    private val _tableState = MutableStateFlow<ResourceUiState<List<TableCardData>>>(ResourceUiState.Loading)
    val tableState: StateFlow<ResourceUiState<List<TableCardData>>> = _tableState.asStateFlow()

    private val _floorState = MutableStateFlow<List<TableFilterData>>(emptyList())
    val floorState: StateFlow<List<TableFilterData>> = _floorState.asStateFlow()

    var originalTableList: List<TableCardData> = emptyList()
        private set

    // Simplified initial state invocation
    init {
        Log.d(TAG, "🏗️ WaiterTableViewModel Initializing instance.")
    }

    /**
     * Explicit entry point triggered straight from Fragment's onViewCreated cycle
     */
    fun loadDashboardData(managerId: String?) {
        if (managerId.isNullOrEmpty()) {
            Log.w(TAG, "loadDashboardData: managerId is null or empty. Aborting data fetch.")
            _tableState.value = ResourceUiState.Error("Missing session credentials")
            return
        }

        Log.d(TAG, "loadDashboardData: Fetching data components for Manager ID: $managerId")
        fetchFloors(managerId)
        fetchTables(managerId)
    }

    private fun fetchFloors(managerId: String) {
        viewModelScope.launch {
            repository.getFloors(managerId)
                .catch { exception ->
                    Log.e(TAG, "Error collecting floors: ${exception.message}")
                    _floorState.value = emptyList()
                }
                .collect { floors ->
                    Log.i(TAG, "Collected floors list update. Elements: ${floors.size}")
                    _floorState.value = floors
                }
        }
    }

    private fun fetchTables(managerId: String) {
        viewModelScope.launch {
            repository.getTables(managerId)
                .catch { exception ->
                    Log.e(TAG, "Error collecting tables pipeline: ${exception.message}")
                    _tableState.value = ResourceUiState.Error(exception.message ?: "Unknown fetch error")
                }
                .collect { resourceUiState ->
                    when (resourceUiState) {
                        is ResourceUiState.Success -> {
                            Log.i(TAG, "Tables Success: Loaded ${resourceUiState.data.size} items.")
                            originalTableList = resourceUiState.data
                        }
                        is ResourceUiState.Error -> {
                            Log.e(TAG, "Tables Error state intercepted: ${resourceUiState.message}")
                        }
                        ResourceUiState.Loading -> {
                            Log.d(TAG, "Tables stream state updating: [Loading]")
                        }
                        else -> {}
                    }
                    _tableState.value = resourceUiState
                }
        }
    }
}