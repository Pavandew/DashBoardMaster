package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.repo.TableManagementRepository
import com.example.masterdashboard.manager_single_res_dash.uistate.TableItemUiState
import com.example.masterdashboard.manager_single_res_dash.uistate.TableUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TableManagementViewModel : ViewModel() {

    companion object {
        private const val TAG = "TableManagementViewModel ----> "
    }

    private val repository = TableManagementRepository()

    private val _floorUiState = MutableStateFlow<TableUiState>(TableUiState.Loading)
    val floorUiState: StateFlow<TableUiState> = _floorUiState.asStateFlow()

    private val _tableItemUiState = MutableStateFlow<TableItemUiState>(TableItemUiState.Loading)
    val tableItemUiState: StateFlow<TableItemUiState> = _tableItemUiState.asStateFlow()

    fun observeFloors(ownerUid: String) {
        Log.d(TAG, "observeFloors called for Owner UID: $ownerUid")

        viewModelScope.launch {
            _floorUiState.value = TableUiState.Loading

            repository.getFloors(ownerUid = ownerUid)
                .catch { exception ->
                    Log.e(TAG, "Error while observing floors: ${exception.message}", exception)
                    _floorUiState.value = TableUiState.Error(exception.message ?: "Failed to synchronize floor records")
                }
                .collect { list ->
                    Log.d(TAG, "Received floors update: ${list.size} floors found")
                    if (list.isEmpty()) {
                        Log.w(TAG, "Real-time stream returned an empty floor list.")
                        _floorUiState.value = TableUiState.Empty
                    } else {
                        list.forEach { floor ->
                            Log.v(TAG, "   └─ Floor item: [ID: ${floor.floorId} | Name: ${floor.floorName} | Tables: ${floor.tableCount}]")
                        }
                        _floorUiState.value = TableUiState.Success(list)
                    }
                }
        }
    }

    fun addNewFloors(
        ownerUid: String,
        floorName: String,
        currentListSize: Int
    ) {
        Log.i(TAG, "➕ Attempting to add new floor: '$floorName' for Owner: $ownerUid")
        viewModelScope.launch {
            try {
                // Passes the current list size to auto-calculate the display sequencing index layout
                repository.storeNewFloor(
                    ownerUid = ownerUid,
                    floorName = floorName,
                    nextDisplayOrder = currentListSize + 1
                )
                Log.i(TAG, "Successfully added new floor: '$floorName' for Owner: $ownerUid")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add new floor: '$floorName' for Owner: $ownerUid. Error: ${e.message}", e)
            }
        }
    }

    /**
     * Synchronizes and computes analytical real-time tracking streams for tables under a floor.
     */
    fun observeTables(ownerUid: String, floorId: String) {
        Log.d(TAG, "observeTables initiated for Floor ID: $floorId")
        viewModelScope.launch {
            _tableItemUiState.value = TableItemUiState.Loading

            repository.getLiveTables(ownerUid = ownerUid, floorId = floorId)
                .catch { exception ->
                    Log.e(TAG, "Error while observing tables subcollection: ${exception.message}", exception)
                    _tableItemUiState.value = TableItemUiState.Error(exception.message ?: "Failed to sync tables.")
                }
                .collect { list ->
                    Log.d(TAG, "Received tables layout push update containing ${list.size} rows.")
                    if (list.isEmpty()) {
                        _tableItemUiState.value = TableItemUiState.Empty
                    } else {
                        // Dynamically compute layout metric card values from snapshot list array on-the-fly
                        val total = list.size
                        val available = list.count { it.status.uppercase() == "AVAILABLE" }
                        val occupied = list.count { it.status.uppercase() == "OCCUPIED" }

                        _tableItemUiState.value = TableItemUiState.Success(
                            tables = list,
                            totalCount = total,
                            availableCount = available,
                            occupiedCount = occupied
                        )
                    }
                }
        }
    }
    /**
     * Dispatches a floor cascading deletion job payload via repository suspend routines.
     */
    fun deleteFloorItem(
        ownerUid: String,
        floorId: String,
        floorName: String
    ) {
        Log.i(TAG, "Initiating cascading deletion request for floor: '$floorName'")

        viewModelScope.launch {
            try {
                // Delegate direct database data mutations safely to the repository layer
                repository.removeFloorCascading(ownerUid = ownerUid, floorId = floorId)
                Log.i(TAG, "Successfully completed deletion sequence for floor: '$floorName'")
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to complete cascade deletion sequence for floor: '$floorName'. Error: ${exception.message}", exception)
            }
        }
    }
}