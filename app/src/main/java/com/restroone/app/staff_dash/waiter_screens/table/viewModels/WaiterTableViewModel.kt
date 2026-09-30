package com.restroone.app.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restroone.app.staff_dash.waiter_screens.table.models.TableCardData
import com.restroone.app.staff_dash.waiter_screens.table.models.TableFilterData
import com.restroone.app.staff_dash.waiter_screens.table.models.TableStatus
import com.restroone.app.staff_dash.waiter_screens.table.uistate.WaiterTablesUiState
import com.restroone.app.staff_dash.waiter_screens.table.repo.WaiterTableRepository
import com.restroone.app.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(WaiterTablesUiState(isLoading = true))
    val uiState: StateFlow<WaiterTablesUiState> = _uiState.asStateFlow()

    private val _rawTables = MutableStateFlow<ResourceUiState<List<TableCardData>>>(ResourceUiState.Loading)
    private val _rawFloors = MutableStateFlow<List<TableFilterData>>(emptyList())
    private val _selectedFloorId = MutableStateFlow("ALL_FLOORS")

    var originalTableList: List<TableCardData> = emptyList()
        private set

    init {
        Log.d(TAG, "🏗️ WaiterTableViewModel Initializing instance.")

        viewModelScope.launch {
            combine(_rawTables, _rawFloors, _selectedFloorId) { tablesResource, floors, selectedId ->
                val mappedFloors = floors.map { it.copy(isSelected = it.id == selectedId) }

                when (tablesResource) {
                    is ResourceUiState.Loading -> {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = current.tables.isEmpty() && !current.isRefreshing,
                                selectedFloorId = selectedId,
                                floors = mappedFloors
                            )
                        }
                    }
                    is ResourceUiState.Success -> {
                        val filtered = if (selectedId == "ALL_FLOORS") {
                            tablesResource.data
                        } else {
                            tablesResource.data.filter { it.floorId == selectedId }
                        }
                        val sorted = filtered.sortedWith(compareBy<TableCardData> { it.floorName }
                            .thenBy { extractInt(it.tableName) })

                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                selectedFloorId = selectedId,
                                floors = mappedFloors,
                                tables = sorted,
                                errorMessage = null
                            )
                        }
                    }
                    is ResourceUiState.Error -> {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                isRefreshing = false,
                                selectedFloorId = selectedId,
                                floors = mappedFloors,
                                errorMessage = if (current.tables.isEmpty()) tablesResource.message else null
                            )
                        }
                    }
                    else -> {}
                }
            }.collect {}
        }
    }

    private fun extractInt(s: String): Int {
        val num = s.replace("\\D".toRegex(), "")
        return if (num.isEmpty()) 0 else num.toInt()
    }

    fun loadDashboardData(managerId: String?) {
        if (managerId.isNullOrEmpty()) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = "Missing session credentials") }
            return
        }

        if (originalTableList.isEmpty()) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            fetchFloors(managerId)
            fetchTables(managerId)
        }
    }

    fun forceRefresh(managerId: String?) {
        if (managerId.isNullOrEmpty()) return
        Log.d(TAG, "forceRefresh: Re-triggering floor and table flows for Manager ID: $managerId")
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        originalTableList = emptyList()
        _rawTables.value = ResourceUiState.Loading
        fetchFloors(managerId)
        fetchTables(managerId)
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
