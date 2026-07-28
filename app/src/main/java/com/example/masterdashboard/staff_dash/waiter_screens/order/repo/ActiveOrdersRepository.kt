package com.example.masterdashboard.staff_dash.waiter_screens.order.repo

import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderDataModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // FIX 1: Required import for .await()
import java.text.SimpleDateFormat
import java.util.Locale

class ActiveOrdersRepository {
    companion object {
        private const val TAG = "Order_Flow_Debug"
    }

    private val firestore = FirebaseFirestore.getInstance()

    fun fetchLiveActiveOrders(managerId: String?): Flow<ResourceUiState<List<ActiveOrderCardData>>> = callbackFlow {
        Log.d(TAG, "📦 [REPO] fetchLiveActiveOrders() invoked for Manager ID: $managerId")
        trySend(ResourceUiState.Loading)

        if (managerId.isNullOrEmpty()) {
            Log.e(TAG, "📦 [REPO] Error: Manager ID is null or empty.")
            trySend(ResourceUiState.Error("Manager ID is null or empty"))
            close()
            return@callbackFlow
        }

        val ordersQuery = firestore.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)

        val listener = ordersQuery.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e(TAG, "📦 [REPO] Snapshot listener error: ${exception.message}", exception)
                trySend(ResourceUiState.Error("Error fetching active orders: ${exception.message}"))
                return@addSnapshotListener
            }

            val totalDocs = snapshots?.documents?.size ?: 0
            Log.i(TAG, "📦 [REPO] Snapshot event received. Total active_orders documents found in Firestore: $totalDocs")

            if (totalDocs == 0) {
                // FIX 2: Explicitly type emptyList to prevent type inference errors
                trySend(ResourceUiState.Success(emptyList<ActiveOrderCardData>()))
                return@addSnapshotListener
            }

            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

            // FIX 3: Launch safely using CoroutineScope(Dispatchers.IO) so .await() is legal
            CoroutineScope(Dispatchers.IO).launch {
                val activeOrderList = mutableListOf<ActiveOrderCardData>()

                snapshots?.documents?.forEachIndexed { index, document ->
                    val docPath = document.reference.path
                    Log.d(TAG, "--------------------------------------------------")
                    Log.d(TAG, "📦 [REPO] Processing Document [$index]: Path -> $docPath")

                    if (!docPath.contains("users/$managerId")) {
                        Log.w(TAG, "📦 [REPO] Skipping Doc [$index]: Does not belong to manager $managerId")
                        return@forEachIndexed
                    }

                    val rawDataMap = document.data
                    Log.d(TAG, "📦 [REPO] Doc ID: ${document.id} | Raw Firestore Data: $rawDataMap")

                    val orderModel = try {
                        document.toObject(OrderDataModel::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "📦 [REPO] Crash converting doc ${document.id} to OrderDataModel", e)
                        null
                    }

                    if (orderModel != null) {
                        // 1. RESOLVE DISPLAY ORDER ID
                        val customDocOrderId = document.getString("orderId")
                        val finalOrderId = when {
                            !customDocOrderId.isNullOrBlank() -> customDocOrderId
                            !orderModel.orderId.isNullOrBlank() -> orderModel.orderId
                            else -> document.id
                        }

                        // 2. RESOLVE TABLE NAME
                        var resolvedTableName = document.getString("tableName") ?: orderModel.tableName

                        if (resolvedTableName.isBlank() || resolvedTableName == "N/A") {
                            val parentTableRef = document.reference.parent.parent
                            if (parentTableRef != null) {
                                try {
                                    // .await() works cleanly inside CoroutineScope.launch!
                                    val parentSnapshot = parentTableRef.get().await()
                                    resolvedTableName = parentSnapshot.getString("tableName") ?: "Table"
                                    Log.d(TAG, "📦 [REPO] Asynchronously fetched parent table name: '$resolvedTableName'")
                                } catch (e: Exception) {
                                    Log.e(TAG, "📦 [REPO] Failed to fetch parent table document", e)
                                    resolvedTableName = "Table"
                                }
                            } else {
                                resolvedTableName = "Table"
                            }
                        }

                        // 3. COMPUTE TOTAL ITEM COUNT
                        val totalItemCount = if (orderModel.items.isNotEmpty()) {
                            orderModel.items.sumOf { it.quantity }
                        } else {
                            0
                        }

                        // 4. FORMAT TIMESTAMP
                        val formattedTime = try {
                            formatter.format(orderModel.timestamp.toDate())
                        } catch (e: Exception) {
                            "Just Now"
                        }

                        // 5. MAP STATUS
                        val statusStr = document.getString("orderStatus") ?: orderModel.orderStatus
                        val status = when (statusStr.uppercase()) {
                            "PENDING" -> ActiveOrderStatus.PENDING      // Maps directly to PENDING!
                            "PREPARING" -> ActiveOrderStatus.PREPARING  // Maps to PREPARING
                            "READY" -> ActiveOrderStatus.READY          // Maps to READY
                            "SERVED" -> ActiveOrderStatus.SERVED        // Maps to SERVED
                            "BILLING" -> ActiveOrderStatus.BILLING      // Maps to BILLING
                            "PAID" -> ActiveOrderStatus.PAID            // Maps to PAID
                            else -> ActiveOrderStatus.PENDING           // Safe fallback
                        }

                        val cardData = ActiveOrderCardData(
                            orderId = finalOrderId,
                            tableName = resolvedTableName,
                            totalItems = totalItemCount,
                            orderTime = formattedTime,
                            status = status
                        )

                        Log.i(TAG, "📦 [REPO] Final ActiveOrderCardData Built -> $cardData")
                        activeOrderList.add(cardData)
                    }
                }

                Log.i(TAG, "==================================================")
                Log.i(TAG, "📦 [REPO] Emitting Success with ${activeOrderList.size} processed active order cards to ViewModel.")
                trySend(ResourceUiState.Success(activeOrderList))
            }
        }

        awaitClose {
            Log.d(TAG, "📦 [REPO] Active orders snapshot listener removed.")
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

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
                Log.i(TAG, "📦 [REPO] Order $orderId status successfully updated to ${newStatus.name}")
                trySend(ResourceUiState.Success(true))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "📦 [REPO] Failed to update order status for $orderId", e)
                trySend(ResourceUiState.Error(e.message ?: "Failed to update status"))
                close(e)
            }

        awaitClose { }
    }.flowOn(Dispatchers.IO)
}