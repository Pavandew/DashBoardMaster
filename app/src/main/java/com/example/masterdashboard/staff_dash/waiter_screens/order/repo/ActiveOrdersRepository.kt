package com.example.masterdashboard.staff_dash.waiter_screens.order.repo

import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.collections.emptyList

class ActiveOrdersRepository {
    companion object {
        private const val TAG = "ActiveOrdersRepository"
    }

    private val firestore = FirebaseFirestore.getInstance()

    // simulates an API call fetching category filters
    fun fetchLiveActiveOrders(managerId: String?): Flow<ResourceUiState<List<ActiveOrderCardData>>> = callbackFlow {
        trySend(ResourceUiState.Loading)

        if(managerId.isNullOrEmpty()) {
            trySend(ResourceUiState.Error("Manager ID is null or empty"))
            close()
            return@callbackFlow
        }

        // Listens to the 'active_order; sub-collection across all tables in real-time
        val ordersQuery = firestore.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)

        val listener = ordersQuery.addSnapshotListener { snapshots, exception ->
            if(exception != null) {
                Log.e(TAG, "Error fetching active orders: ${exception.message}", exception)
                trySend(ResourceUiState.Error("Error fetching active orders: ${exception.message}"))
                return@addSnapshotListener

            }

            val activeOrderList = mutableListOf<ActiveOrderCardData>()
            snapshots?.documents?.forEach { document ->
                val orderId = document.id
                val tableName = document.getString("tableName") ?: "Unknown Table"
                val items = document.get("items") as? List<*> ?: emptyList<Any>()
                val itemCount = items.size

                // Format timestamp or string time
                val timeString = document.getString("time") ?: "Unknown Time"
                // Map status string to enum
                val statusStr = document.getString("orderStatus") ?: "PREPARING"
                val status = try {
                    ActiveOrderStatus.valueOf(statusStr.uppercase())
                } catch (e: Exception) {
                    ActiveOrderStatus.PREPARING
                }
                activeOrderList.add(
                    ActiveOrderCardData(
                        orderId = orderId,
                        tableName = tableName,
                        totalItems = itemCount,
                        orderTime = timeString,
                        status = status
                    )
                )
            }
            Log.d(TAG, "Fetched ${activeOrderList.size} live active orders.")
            trySend(ResourceUiState.Success(activeOrderList))
        }
        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)


    // Updates an order's status ( e.g., from PREPARING to READY or SERVED)
    fun updateOrderStatus(
        managerId: String,
        floorId: String,
        tableId: String,
        orderId: String,
        newStatus: ActiveOrderStatus
    ): Flow<ResourceUiState<Boolean>> = callbackFlow {
        trySend(ResourceUiState.Loading)

        val orderRef = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_RES_FLOORS)
            .document(floorId)
            .collection(AppConstants.COLLECTION_TABLES)
            .document(tableId)
            .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
            .document(orderId)

        orderRef.update("orderStatus", newStatus.name)
            .addOnSuccessListener {
                trySend(ResourceUiState.Success(true))
                close()
            }
            .addOnFailureListener { e ->
                trySend(ResourceUiState.Error(e.message ?: "Failed to update status"))
                close(e)
            }

        awaitClose { }
    }.flowOn(Dispatchers.IO)
}