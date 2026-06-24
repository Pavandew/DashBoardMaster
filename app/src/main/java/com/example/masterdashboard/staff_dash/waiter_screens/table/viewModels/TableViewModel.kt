package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.TableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TableViewModel(
    private val repository: TableRepository
) : ViewModel() {

    class TableViewModelFactory(private val repository: TableRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TableViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TableViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val TAG = "TableViewModel"
    }

    private val _tableState =
        MutableStateFlow<ResourceUiState<List<TableCardData>>>(ResourceUiState.Loading)
    val tableState: StateFlow<ResourceUiState<List<TableCardData>>> = _tableState.asStateFlow()

    var originalTableList: List<TableCardData> = emptyList()
        private set

    init {
        fetchTables()
    }

    fun fetchTables() {
        Log.d(TAG, "Starting to fetch tables...")

        // Explicitly launching a coroutine on the Main thread dispatcher
        viewModelScope.launch {
            repository.getTables().collect { resourceUiState ->
                when (resourceUiState) {
                    is ResourceUiState.Loading -> {
                        Log.d(TAG, "Loading tables...")
                        _tableState.value = resourceUiState
                    }

                    is ResourceUiState.Success<List<TableCardData>> -> {
                        Log.d(TAG, "Tables fetched successfully: ${resourceUiState.data.size} tables")
                        originalTableList = resourceUiState.data
                        _tableState.value = resourceUiState
                    }

                    is ResourceUiState.Error -> {
                        Log.e(TAG, "fetchTables State: Error! Msg: ${resourceUiState.message}")
                        _tableState.value = resourceUiState
                    }
                }
            }
        }
    }
}