package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.WaiterTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    private val _rawTables = MutableStateFlow<ResourceUiState<List<TableCardData>>>(ResourceUiState.Loading)
    private val _rawFloors = MutableStateFlow<List<TableFilterData>>(emptyList())
    private val _selectedFloorId = MutableStateFlow("ALL_FLOORS")

    // Reactive pipeline for tables: combines raw tables with the selected floor filter
    val tableState: StateFlow<ResourceUiState<List<TableCardData>>> = combine(_rawTables, _selectedFloorId) { tablesResource, selectedId ->
        if (tablesResource is ResourceUiState.Success) {
            val filtered = if (selectedId == "ALL_FLOORS") {
                tablesResource.data
            } else {
                tablesResource.data.filter { it.floorId == selectedId }
            }
            // Smart sorting: Sort by floor name first (to group floors), then by table name numerically
            val sorted = filtered.sortedWith(compareBy<TableCardData> { it.floorName }
                .thenBy { extractInt(it.tableName) })
            ResourceUiState.Success(sorted)
        } else {
            tablesResource
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResourceUiState.Loading)

    private fun extractInt(s: String): Int {
        val num = s.replace("\\D".toRegex(), "")
        return if (num.isEmpty()) 0 else num.toInt()
    }

    // Reactive pipeline for floors: combines raw floors with the selection state
    val floorState: StateFlow<List<TableFilterData>> = combine(_rawFloors, _selectedFloorId) { floors, selectedId ->
        floors.map { it.copy(isSelected = it.id == selectedId) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedFloorId: StateFlow<String> = _selectedFloorId.asStateFlow()

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
            _rawTables.value = ResourceUiState.Error("Missing session credentials")
            return
        }

        // Only fetch if we don't have data yet
        if (originalTableList.isEmpty()) {
            Log.d(TAG, "loadDashboardData: Fetching data components for Manager ID: $managerId")
            fetchFloors(managerId)
            fetchTables(managerId)
        }
    }

    fun setFloorFilter(floorId: String) {
        _selectedFloorId.value = floorId
    }

    fun updateTableStatus(managerId: String, floorId: String, tableId: String, newStatus: TableStatus, customerName: String? = null) {
        repository.updateTableStatus(managerId, floorId, tableId, newStatus, customerName)
    }

    private fun fetchFloors(managerId: String) {
        viewModelScope.launch {
            repository.getFloors(managerId)
                .catch { exception ->
                    Log.e(TAG, "Error collecting floors: ${exception.message}")
                    _rawFloors.value = emptyList()
                }
                .collect { floors ->
                    Log.i(TAG, "Collected floors list update. Elements: ${floors.size}")
                    _rawFloors.value = floors
                }
        }
    }

    private fun fetchTables(managerId: String) {
        viewModelScope.launch {
            repository.getTables(managerId)
                .catch { exception ->
                    Log.e(TAG, "Error collecting tables pipeline: ${exception.message}")
                    _rawTables.value = ResourceUiState.Error(exception.message ?: "Unknown fetch error")
                }
                .collect { resourceUiState ->
                    if (resourceUiState is ResourceUiState.Success) {
                        originalTableList = resourceUiState.data
                    }
                    _rawTables.value = resourceUiState
                }
        }
    }
}