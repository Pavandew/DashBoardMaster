package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

class WaiterTableRepository {

    companion object {
        private const val TAG = "Table_Flow_Debug"
    }
    private val firestore = FirebaseFirestore.getInstance()

    // 1. Simple, direct fetch for floors
    fun getFloors(managerId: String?): Flow<List<TableFilterData>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] getFloors() called for Manager ID: $managerId")

        if (managerId.isNullOrEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val floorsRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_RES_FLOORS)

        val listener = floorsRef.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e(TAG, "📦 [REPO] Floors snapshot error", exception)
                close(exception)
                return@addSnapshotListener
            }

            val floorList = mutableListOf<TableFilterData>()
            floorList.add(TableFilterData(id = "ALL_FLOORS", name = "All", isSelected = true))

            snapshots?.documents?.forEach { doc ->
                val id = doc.id
                val name = doc.getString(AppConstants.FIELD_FLOOR_NAME) ?: "Unnamed Floor"
                if (name.lowercase() != "all") {
                    floorList.add(TableFilterData(id = id, name = name, isSelected = false))
                }
            }
            trySend(floorList)
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    // 2. Simple, direct fetch for tables using explicit sub-collections
    fun getTables(managerId: String?): Flow<ResourceUiState<List<TableCardData>>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] getTables() initiated for Manager ID: $managerId")
        trySend(ResourceUiState.Loading)

        if (managerId.isNullOrEmpty()) {
            trySend(ResourceUiState.Error("Invalid Manager ID Session"))
            close()
            return@callbackFlow
        }

        // Keep track of active sub-collection listeners so we can clear them later
        val activeListeners = mutableListOf<ListenerRegistration>()

        // A map to hold table arrays grouped by their floorId
        val tablesMap = mutableMapOf<String, List<TableCardData>>()

        // Step A: First, get the floors so we know exactly which sub-collections exist
        val floorsRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_RES_FLOORS)

        val masterFloorsListener = floorsRef.addSnapshotListener { floorSnapshots, floorException ->
            if (floorException != null) {
                trySend(ResourceUiState.Error(floorException.message ?: "Error getting floors"))
                return@addSnapshotListener
            }

            // Clear old sub-listeners if the floor structure changes
            activeListeners.forEach { it.remove() }
            activeListeners.clear()

            val floorDocs = floorSnapshots?.documents ?: emptyList()
            if (floorDocs.isEmpty()) {
                trySend(ResourceUiState.Success(emptyList()))
                return@addSnapshotListener
            }

            // Step B: Loop through every single floor document found
            floorDocs.forEach { floorDoc ->
                val floorId = floorDoc.id

                // Point directly to the nested sub-collection: users -> {uid} -> res_floors -> {floorId} -> floor_tables
                val tablesRef = floorsRef.document(floorId).collection(AppConstants.COLLECTION_TABLES)

                val tableListener = tablesRef.addSnapshotListener { tableSnapshots, tableException ->
                    if (tableException != null) {
                        Log.e(TAG, "Error matching sub-collection path tables for floor: $floorId")
                        return@addSnapshotListener
                    }

                    val singleFloorTablesList = mutableListOf<TableCardData>()

                    tableSnapshots?.documents?.forEach { doc ->
                        try {
                            val tableId = doc.getString(AppConstants.FIELD_TABLE_ID) ?: doc.id
                            val tableName = doc.getString(AppConstants.FIELD_TABLE_NAME) ?: "Unknown Table"
                            val totalSeats = doc.getLong(AppConstants.FIELD_TOTAL_SEATS)?.toInt() ?: 4
                            val statusString = doc.getString(AppConstants.FIELD_STATUS) ?: AppConstants.STATUS_FREE
                            val status = try {
                                TableStatus.valueOf(statusString.uppercase())
                            } catch (e: Exception) {
                                TableStatus.FREE
                            }
                            val currentBillAmount = doc.getString(AppConstants.FIELD_CURRENT_BILL)

                            singleFloorTablesList.add(
                                TableCardData(tableId, tableName, floorId, totalSeats, status, currentBillAmount)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing table object doc: ${doc.id}", e)
                        }
                    }

                    // Save this floor's loaded tables into our map tracking structure
                    tablesMap[floorId] = singleFloorTablesList

                    // Step C: Flatten the map into one combined list and send it to the UI Fragment!
                    val combinedMasterList = tablesMap.values.flatten()
                    Log.i(TAG, "📦 [REPO] Pushing updated flattened master list size: ${combinedMasterList.size} items to UI.")
                    trySend(ResourceUiState.Success(combinedMasterList))
                }

                activeListeners.add(tableListener)
            }
        }

        // Clean up everything when the user leaves the fragment screen
        awaitClose {
            Log.d(TAG, "📦 [REPO] Removing all active nested sub-collection table snapshot listeners.")
            masterFloorsListener.remove()
            activeListeners.forEach { it.remove() }
        }
    }.flowOn(Dispatchers.IO)
}