package com.example.masterdashboard.manager_single_res_dash.home.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.home.repo.TableManagementRepository
import com.example.masterdashboard.manager_single_res_dash.home.uistate.TableItemUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TableItemListViewModel : ViewModel() {

    companion object {
        private const val TAG = "TableItemListViewModel ----> "
    }

    private val repository = TableManagementRepository()

    private val _tableItemUiState = MutableStateFlow<TableItemUiState>(TableItemUiState.Loading)
    val tableItemUiState: StateFlow<TableItemUiState> = _tableItemUiState.asStateFlow()

    // FIXED: Independent state tracking flag protects against cache query resets hiding your loader
    private var isAddingTable: Boolean = false

    /**
     * Synchronizes and computes analytical real-time tracking streams for tables under a floor.
     */
    fun observeTables(ownerUid: String, floorId: String) {
        Log.d(TAG, "observeTables called for Owner: $ownerUid, Floor ID: $floorId")
        viewModelScope.launch {
            // If the state is already Success, we keep it as Success to avoid empty screen flashing[cite: 1686].
            if (_tableItemUiState.value !is TableItemUiState.Success) {
                _tableItemUiState.value = TableItemUiState.Loading
            }

            repository.getLiveTables(ownerUid = ownerUid, floorId = floorId)
                .catch { exception ->
                    Log.e(TAG, "Error while observing tables subcollection: ${exception.message}", exception)
                    _tableItemUiState.value = TableItemUiState.Error(exception.message ?: "Failed to sync tables records")
                }
                .collect { list ->
                    Log.d(TAG, "Received tables push update: ${list.size} rows found.")

                    if (list.isEmpty()) {
                        Log.w(TAG, "Real-time table stream returned an empty list.")
                        _tableItemUiState.value = TableItemUiState.Empty
                    } else {
                        // 1. NATURAL NUMERIC SORTING FIX:
                        // Extracts numeric characters from your tableName String (e.g., "T12" -> 12)
                        val naturalSortedList = list.sortedWith(compareBy { table ->
                            val numericPart = "[0-9]+".toRegex().find(table.tableName)?.value
                            numericPart?.toIntOrNull() ?: Int.MAX_VALUE
                        })

                        val total = naturalSortedList.size
                        val available = naturalSortedList.count { it.status.uppercase() == "AVAILABLE" }
                        val occupied = naturalSortedList.count { it.status.uppercase() == "OCCUPIED" }

                        // 2. FIXED TARGET ASSIGNMENT VARIABlE:
                        // Passes the 'naturalSortedList' and couples the active background 'isAddingTable' flag
                        _tableItemUiState.value = TableItemUiState.Success(
                            tables = naturalSortedList,
                            totalCount = total,
                            availableCount = available,
                            occupiedCount = occupied,
                            isRefreshing = isAddingTable
                        )
                    }
                }
        }
    }

    /**
     * Dispatches a table creation mutation job payload downstream via repository suspend routines.
     */
    fun addNewTable(ownerUid: String, floorId: String, tableName: String, capacity: Int, status: String) {
        Log.i(TAG, "➕ Attempting to add table '$tableName' to Floor: $floorId")

        // Mark persistent variable as active before running background tasks
        isAddingTable = true
        updateRefreshingState(true)

        viewModelScope.launch {
            try {
                repository.storeNewTable(
                    ownerUid = ownerUid,
                    floorId = floorId,
                    tableName = tableName,
                    capacity = capacity,
                    tableStatus = status
                )
                Log.i(TAG, "✅ storeNewTable transaction completed successfully on server.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to complete table addition task. Error: ${e.message}", e)
            } finally {
                // This block runs ONLY when the cloud database transaction has completed successfully [cite: 1729]
                isAddingTable = false
                updateRefreshingState(false)
                Log.d(TAG, "Background sync fully completed. Progress bar cleared safely.")
            }
        }
    }

    /**
     * Internal helper method that updates the active refresh progress flags smoothly.
     */
    private fun updateRefreshingState(refreshing: Boolean) {
        val currentState = _tableItemUiState.value
        if (currentState is TableItemUiState.Success) {
            _tableItemUiState.value = currentState.copy(isRefreshing = refreshing)
        }
    }

    /**
     * Dispatches a table deletion job payload via repository suspend routines.
     */
    fun deleteTableItem(ownerUid: String, floorId: String, tableId: String, tableName: String) {
        Log.i(TAG, "Initiating transaction deletion request for table: '$tableName'")
        viewModelScope.launch {
            try {
                repository.removeTableTransactional(ownerUid = ownerUid, floorId = floorId, tableId = tableId)
                Log.i(TAG, "Successfully completed deletion sequence for table: '$tableName'")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to complete deletion sequence for table: '$tableName'. Error: ${exception.message}", exception)
            }
        }
    }
}