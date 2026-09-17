package com.example.masterdashboard.staff_dash.waiter_screens.table.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableCardData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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

        val floorsRef = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .orderBy("displayFloor", Query.Direction.ASCENDING)

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

    // 2. Fetch tables using outlet sub-collections
    fun getTables(managerId: String?): Flow<ResourceUiState<List<TableCardData>>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] getTables() initiated for Manager ID: $managerId")
        trySend(ResourceUiState.Loading)

        if (managerId.isNullOrEmpty()) {
            trySend(ResourceUiState.Error("Invalid Manager ID Session"))
            close()
            return@callbackFlow
        }

        val activeListeners = mutableListOf<ListenerRegistration>()
        val tablesMap = mutableMapOf<String, List<TableCardData>>()

        val floorsRef = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_RES_FLOORS)

        val masterFloorsListener = floorsRef.addSnapshotListener { floorSnapshots, floorException ->
            if (floorException != null) {
                trySend(ResourceUiState.Error(floorException.message ?: "Error getting floors"))
                return@addSnapshotListener
            }

            activeListeners.forEach { it.remove() }
            activeListeners.clear()

            val floorDocs = floorSnapshots?.documents ?: emptyList()
            if (floorDocs.isEmpty()) {
                trySend(ResourceUiState.Success(emptyList()))
                return@addSnapshotListener
            }

            floorDocs.forEach { floorDoc ->
                val floorId = floorDoc.id
                val floorName = floorDoc.getString(AppConstants.FIELD_FLOOR_NAME) ?: "Unknown Floor"

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
                            
                            val normalized = statusString.uppercase().trim()
                            val status = when (normalized) {
                                "FREE", "AVAILABLE" -> TableStatus.FREE
                                "OCCUPIED", "BUSY" -> TableStatus.OCCUPIED
                                "RESERVED", "BOOKED" -> TableStatus.RESERVED
                                "BILLING", "CHECKOUT" -> TableStatus.BILLING
                                "PAID", "SUCCESS", "COMPLETED" -> TableStatus.FREE
                                else -> TableStatus.FREE
                            }

                            val customerName = doc.getString(AppConstants.FIELD_CUSTOMER_NAME_TABLE)
                            val rawBill = doc.get(AppConstants.FIELD_CURRENT_BILL)
                            val currentBillAmount = when (rawBill) {
                                is String -> rawBill
                                is Number -> if (rawBill.toDouble() == 0.0) "" else "₹${rawBill.toInt()}"
                                else -> null
                            }

                            singleFloorTablesList.add(
                                TableCardData(tableId, tableName, floorId, floorName, totalSeats, status, customerName, currentBillAmount)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing table object doc: ${doc.id}", e)
                        }
                    }

                    tablesMap[floorId] = singleFloorTablesList

                    val combinedMasterList = tablesMap.values.flatten()
                    Log.i(TAG, "📦 [REPO] Pushing updated flattened master list size: ${combinedMasterList.size} items to UI.")
                    trySend(ResourceUiState.Success(combinedMasterList))
                }

                activeListeners.add(tableListener)
            }
        }

        awaitClose {
            Log.d(TAG, "📦 [REPO] Removing all active nested sub-collection table snapshot listeners.")
            masterFloorsListener.remove()
            activeListeners.forEach { it.remove() }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Updates the status and customer name of a specific table.
     */
    fun updateTableStatus(managerId: String, floorId: String, tableId: String, newStatus: TableStatus, customerName: String? = null) {
        val tableRef = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .document(floorId)
            .collection(AppConstants.COLLECTION_TABLES)
            .document(tableId)

        val updates = mutableMapOf<String, Any?>(
            AppConstants.FIELD_STATUS to newStatus.name
        )
        
        if (newStatus == TableStatus.FREE) {
            updates[AppConstants.FIELD_CUSTOMER_NAME_TABLE] = null
            updates[AppConstants.FIELD_CURRENT_BILL] = null
        } else if (customerName != null) {
            updates[AppConstants.FIELD_CUSTOMER_NAME_TABLE] = customerName
        }

        tableRef.update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully updated table $tableId to ${newStatus.name}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating table status", e)
            }
    }
}
