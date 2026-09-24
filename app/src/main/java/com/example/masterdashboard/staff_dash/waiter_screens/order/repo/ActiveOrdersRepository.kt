package com.example.masterdashboard.staff_dash.waiter_screens.order.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderDataModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.staff_dash.utils.TimeUtils
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

        var activeOrdersMap = mapOf<String, Pair<ActiveOrderCardData, Long>>()
        var completedOrdersMap = mapOf<String, Pair<ActiveOrderCardData, Long>>()

        fun emitCombinedList() {
            CoroutineScope(Dispatchers.IO).launch {
                val mergedMap = mutableMapOf<String, Pair<ActiveOrderCardData, Long>>()
                // Active orders first
                mergedMap.putAll(activeOrdersMap)
                // Completed / Paid orders merged in
                completedOrdersMap.forEach { (id, pair) ->
                    if (!mergedMap.containsKey(id) || mergedMap[id]?.first?.status != ActiveOrderStatus.PAID) {
                        mergedMap[id] = pair
                    }
                }

                val sortedList = mergedMap.values.sortedByDescending { it.second }.map { it.first }
                Log.i(TAG, "📦 [REPO] Emitting Success with ${sortedList.size} combined active + paid order cards.")
                trySend(ResourceUiState.Success(sortedList))
            }
        }

        // 1. Listen to active_orders subcollections across table locations
        val ordersQuery = firestore.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)
        val activeListener = ordersQuery.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e(TAG, "📦 [REPO] Snapshot listener error: ${exception.message}", exception)
                trySend(ResourceUiState.Error("Error fetching active orders: ${exception.message}"))
                return@addSnapshotListener
            }

            val docs = snapshots?.documents ?: emptyList()
            val tempMap = mutableMapOf<String, Pair<ActiveOrderCardData, Long>>()

            docs.forEach { document ->
                val docPath = document.reference.path
                if (!docPath.contains("${AppConstants.COLLECTION_RESTAURANTS}/$managerId")) {
                    return@forEach
                }

                val orderModel = try {
                    document.toObject(OrderDataModel::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "📦 [REPO] Crash converting doc ${document.id} to OrderDataModel", e)
                    null
                }

                if (orderModel != null) {
                    val customDocOrderId = document.getString(AppConstants.FIELD_ORDER_ID)
                    val finalOrderId = when {
                        !customDocOrderId.isNullOrBlank() -> customDocOrderId
                        !orderModel.orderId.isNullOrBlank() -> orderModel.orderId
                        else -> document.id
                    }

                    var resolvedTableName = document.getString(AppConstants.FIELD_TABLE_NAME) ?: orderModel.tableName
                    if (resolvedTableName.isBlank() || resolvedTableName == "N/A") {
                        resolvedTableName = "Table"
                    }

                    val totalItemCount = if (orderModel.items.isNotEmpty()) {
                        orderModel.items.sumOf { it.quantity }
                    } else {
                        0
                    }

                    val formattedTime = TimeUtils.getRelativeTime(orderModel.timestamp)

                    val statusStr = document.getString(AppConstants.FIELD_ORDER_STATUS) ?: orderModel.orderStatus
                    val status = when (statusStr.uppercase()) {
                        AppConstants.STATUS_PENDING -> ActiveOrderStatus.PENDING
                        AppConstants.STATUS_PREPARING -> ActiveOrderStatus.PREPARING
                        AppConstants.STATUS_READY -> ActiveOrderStatus.READY
                        AppConstants.STATUS_SERVED -> ActiveOrderStatus.SERVED
                        AppConstants.STATUS_BILLING -> ActiveOrderStatus.BILLING
                        AppConstants.STATUS_PAID, AppConstants.STATUS_COMPLETED -> ActiveOrderStatus.PAID
                        else -> ActiveOrderStatus.PENDING
                    }

                    val cardData = ActiveOrderCardData(
                        orderId = finalOrderId,
                        tableName = resolvedTableName,
                        totalItems = totalItemCount,
                        orderTime = formattedTime,
                        status = status
                    )

                    tempMap[finalOrderId] = cardData to orderModel.timestamp.seconds
                }
            }

            activeOrdersMap = tempMap
            emitCombinedList()
        }

        // 2. Listen to completed_orders collection for settled / paid orders
        val completedQuery = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
            .orderBy(AppConstants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)

        val completedListener = completedQuery.addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.e(TAG, "📦 [REPO] Completed orders listener error: ${exception.message}", exception)
                return@addSnapshotListener
            }

            val docs = snapshots?.documents ?: emptyList()
            val tempMap = mutableMapOf<String, Pair<ActiveOrderCardData, Long>>()

            docs.forEach { document ->
                val orderModel = try {
                    document.toObject(OrderDataModel::class.java)
                } catch (e: Exception) {
                    null
                }

                if (orderModel != null) {
                    val customDocOrderId = document.getString(AppConstants.FIELD_ORDER_ID)
                    val finalOrderId = when {
                        !customDocOrderId.isNullOrBlank() -> customDocOrderId
                        !orderModel.orderId.isNullOrBlank() -> orderModel.orderId
                        else -> document.id
                    }

                    var resolvedTableName = document.getString(AppConstants.FIELD_TABLE_NAME) ?: orderModel.tableName
                    if (resolvedTableName.isBlank() || resolvedTableName == "N/A") {
                        resolvedTableName = "Table"
                    }

                    val totalItemCount = if (orderModel.items.isNotEmpty()) {
                        orderModel.items.sumOf { it.quantity }
                    } else {
                        0
                    }

                    val formattedTime = TimeUtils.getRelativeTime(orderModel.timestamp)

                    val cardData = ActiveOrderCardData(
                        orderId = finalOrderId,
                        tableName = resolvedTableName,
                        totalItems = totalItemCount,
                        orderTime = formattedTime,
                        status = ActiveOrderStatus.PAID
                    )

                    tempMap[finalOrderId] = cardData to orderModel.timestamp.seconds
                }
            }

            completedOrdersMap = tempMap
            emitCombinedList()
        }

        awaitClose {
            Log.d(TAG, "📦 [REPO] Removing active and completed orders snapshot listeners.")
            activeListener.remove()
            completedListener.remove()
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

        val isCounterOrder = tableId.isEmpty() || tableId == "COUNTER_ORDER" || tableId == "N/A" || floorId.isEmpty() || floorId == "N/A"

        val orderRef = if (orderId.contains("${AppConstants.COLLECTION_RESTAURANTS}/")) {
            firestore.document(orderId)
        } else if (isCounterOrder) {
            RestaurantPathHelper.getOutletDocRef(managerId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .document(orderId)
        } else {
            RestaurantPathHelper.getOutletDocRef(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .document(orderId)
        }

        orderRef.update(AppConstants.FIELD_ORDER_STATUS, newStatus.name)
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