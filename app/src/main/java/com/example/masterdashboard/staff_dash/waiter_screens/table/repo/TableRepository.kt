package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TableRepository {

    companion object {
        private const val TAG = "TableRepository"
    }

    fun getTables(): Flow<ResourceUiState<List<TableCardData>>> = flow {
        emit(ResourceUiState.Loading)
        Log.d(TAG, "Fetching tables...")

        try {
            delay(500) // Non-blocking coroutine delay safely simulating latency

            val dummyTables = listOf(
                TableCardData("T1", "F1", 4, TableStatus.FREE, "20"),
                TableCardData("T2", "F1", 2, TableStatus.OCCUPIED, "15"),
                TableCardData("T3", "F2", 6, TableStatus.RESERVED, null),
                TableCardData("T4", "F2", 4, TableStatus.BILLING, "50"),
                TableCardData("T1", "F1", 4, TableStatus.FREE, "20"),
                TableCardData("T2", "F1", 2, TableStatus.OCCUPIED, "15"),
                TableCardData("T3", "F2", 6, TableStatus.RESERVED, null),
                TableCardData("T4", "F2", 4, TableStatus.BILLING, "50"),
                TableCardData("T1", "F1", 4, TableStatus.FREE, "20"),
                TableCardData("T2", "F1", 2, TableStatus.OCCUPIED, "15"),
                TableCardData("T3", "F2", 6, TableStatus.RESERVED, null),
                TableCardData("T4", "F2", 4, TableStatus.BILLING, "50")
            )

            Log.d(TAG, "Tables fetched successfully: ${dummyTables.size} tables")
            emit(ResourceUiState.Success(dummyTables))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tables", e)
            emit(ResourceUiState.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO) // Move execution safety boundaries off the main thread
}