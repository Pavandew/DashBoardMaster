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
    val floorState : StateFlow<List<TableFilterData>> = _floorState.asStateFlow()

    var originalTableList: List<TableCardData> = emptyList()
        private set

    init {
        Log.d(TAG, "🏗️ [VIEWMODEL] Initializing WaiterTableViewModel instance.")
        fetchFloors()
        fetchTables()
    }

    fun fetchFloors() {
        Log.d(TAG, "🏗️ [VIEWMODEL] Starting fetchFloors() flow collection.")
        viewModelScope.launch {
            repository.getFloors()
                .catch { exception ->
                    Log.e(TAG, "🏗️ [VIEWMODEL] Catch block intercepted floor pipeline exception: ${exception.message}")
                }
                .collect { floors ->
                    Log.i(TAG, "🏗️ [VIEWMODEL] Collected floor list update from Repository. Pushing ${floors.size} elements to UI State.")
                    _floorState.value = floors
                }
        }
    }

    fun fetchTables() {
        Log.d(TAG, "🏗️ [VIEWMODEL] Starting fetchTables() flow collection.")
        viewModelScope.launch {
            repository.getTables().collect { resourceUiState ->
                when (resourceUiState) {
                    is ResourceUiState.Loading -> {
                        Log.d(TAG, "🏗️ [VIEWMODEL] Table stream state updating: [ResourceUiState.Loading]")
                        _tableState.value = resourceUiState
                    }

                    is ResourceUiState.Success<List<TableCardData>> -> {
                        Log.i(TAG, "🏗️ [VIEWMODEL] Table stream state updating: [ResourceUiState.Success]. Loaded ${resourceUiState.data.size} items.")
                        originalTableList = resourceUiState.data
                        _tableState.value = resourceUiState
                    }

                    is ResourceUiState.Error -> {
                        Log.e(TAG, "🏗️ [VIEWMODEL] Table stream state updating: [ResourceUiState.Error]. Reason: ${resourceUiState.message}")
                        _tableState.value = resourceUiState
                    }
                }
            }
        }
    }
}