package com.example.masterdashboard.staff_dash.waiter_screens.order.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderDetailExpansionUiState
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderExpandedItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderDataModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailRepository {

    companion object {
        private const val TAG = "Order_Detail_Debug"
    }

    private val firestore = FirebaseFirestore.getInstance()

    fun fetchDetailedTicket(
        managerId: String,
        orderId: String,
        fallbackTableName: String
    ): Flow<ResourceUiState<OrderDetailExpansionUiState>> = flow {
        Log.d(TAG, "📦 [REPO] fetchDetailedTicket() started for Manager: $managerId, OrderId: $orderId")
        emit(ResourceUiState.Loading)

        try {
            val querySnapshot = firestore.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .get()
                .await()

            Log.d(TAG, "📦 [REPO] Query completed. Total documents checked across collectionGroup: ${querySnapshot.size()}")

            val document = querySnapshot.documents.firstOrNull { doc ->
                (doc.id == orderId || doc.getString(AppConstants.FIELD_ORDER_ID) == orderId) &&
                        doc.reference.path.contains("users/$managerId")
            }

            if (document != null && document.exists()) {
                Log.i(TAG, "📦 [REPO] Target order document found at path: ${document.reference.path}")
                val orderModel = document.toObject(OrderDataModel::class.java)

                if (orderModel != null) {
                    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

                    // Map food items
                    val mappedItems = orderModel.items.map { item ->
                        OrderExpandedItemData(
                            id = item.itemId,
                            name = item.itemName,
                            variantName = item.variantName, // Added
                            quantity = item.quantity,
                            unitPrice = item.price,
                            totalPrice = if (item.rowTotal > 0) item.rowTotal else item.price * item.quantity,
                            orderedQuantity = item.orderedQuantity,
                            status = item.itemStatus
                        )
                    }

                    // Extract Floor ID and Table ID from reference path
                    val pathSegments = document.reference.path.split("/")
                    var extractedFloorId = ""
                    var extractedTableId = ""

                    for (i in pathSegments.indices) {
                        if (pathSegments[i] == AppConstants.COLLECTION_RES_FLOORS && i + 1 < pathSegments.size) {
                            extractedFloorId = pathSegments[i + 1]
                        }
                        if (pathSegments[i] == AppConstants.COLLECTION_TABLES && i + 1 < pathSegments.size) {
                            extractedTableId = pathSegments[i + 1]
                        }
                    }

                    // Resolve Table Display Name
                    var tableName = document.getString(AppConstants.FIELD_TABLE_NAME) ?: orderModel.tableName
                    if (tableName.isBlank() || tableName == "N/A") {
                        tableName = if (fallbackTableName.isNotBlank()) fallbackTableName else "Table"
                    }

                    // Map status string to enum
                    val statusStr = document.getString(AppConstants.FIELD_ORDER_STATUS) ?: orderModel.orderStatus
                    val status = when (statusStr.uppercase().trim()) {
                        AppConstants.STATUS_PENDING -> ActiveOrderStatus.PENDING
                        AppConstants.STATUS_PREPARING -> ActiveOrderStatus.PREPARING
                        AppConstants.STATUS_READY -> ActiveOrderStatus.READY
                        AppConstants.STATUS_SERVED -> ActiveOrderStatus.SERVED
                        AppConstants.STATUS_BILLING -> ActiveOrderStatus.BILLING
                        AppConstants.STATUS_PAID -> ActiveOrderStatus.PAID
                        else -> ActiveOrderStatus.PENDING
                    }

                    val formattedTime = try {
                        formatter.format(orderModel.timestamp.toDate())
                    } catch (e: Exception) {
                        "Just Now"
                    }

                    val customDisplayOrderId = document.getString(AppConstants.FIELD_ORDER_ID) ?: document.id

                    // Financial Totals (Excluding Rejected items)
                    val activeItems = orderModel.items.filter { !it.itemStatus.equals(AppConstants.STATUS_REJECTED, ignoreCase = true) }
                    val calculatedSubtotal = activeItems.sumOf { if (it.rowTotal > 0) it.rowTotal else it.price * it.quantity }
                    val calculatedGst = calculatedSubtotal * 0.05
                    val calculatedGrandTotal = calculatedSubtotal + calculatedGst

                    val detailPayload = OrderDetailExpansionUiState(
                        isLoading = false,
                        errorMessage = null,
                        orderId = customDisplayOrderId,
                        documentId = document.id,
                        floorId = extractedFloorId,
                        tableId = extractedTableId,
                        tableName = tableName,
                        status = status,
                        timeStamp = formattedTime,
                        items = mappedItems,
                        subtotal = calculatedSubtotal,
                        gstAmount = calculatedGst,
                        grandTotal = calculatedGrandTotal
                    )

                    Log.i(TAG, "📦 [REPO] Successfully parsed OrderDetailExpansionUiState with ${mappedItems.size} items.")
                    emit(ResourceUiState.Success(detailPayload))
                } else {
                    Log.e(TAG, "📦 [REPO] Failed to convert document into OrderDataModel.")
                    emit(ResourceUiState.Error("Failed to parse order details"))
                }
            } else {
                Log.e(TAG, "📦 [REPO] Document for order ID '$orderId' was not found.")
                emit(ResourceUiState.Error("Order document not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Exception fetching order detail", e)
            emit(ResourceUiState.Error("Failed to fetch order details: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateOrderStatusToServed(
        managerId: String,
        floorId: String,
        tableId: String,
        orderDocId: String
    ): Flow<ResourceUiState<Boolean>> = flow {
        Log.d(TAG, "📦 [REPO] Updating Order '$orderDocId' to SERVED and all its items...")
        emit(ResourceUiState.Loading)

        try {
            val orderRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .document(orderDocId)

            val snapshot = orderRef.get().await()
            val orderModel = snapshot.toObject(OrderDataModel::class.java)

            if (orderModel != null) {
                // Mark all items that are not REJECTED as SERVED
                val updatedItems = orderModel.items.map { item ->
                    if (!item.itemStatus.equals(AppConstants.STATUS_REJECTED, ignoreCase = true)) {
                        item.copy(itemStatus = AppConstants.STATUS_SERVED)
                    } else item
                }

                val updates = mapOf(
                    AppConstants.FIELD_ORDER_STATUS to AppConstants.STATUS_SERVED,
                    AppConstants.FIELD_ORDER_ITEMS to updatedItems
                )

                orderRef.update(updates).await()
                Log.i(TAG, "📦 [REPO] Successfully updated order and items to SERVED in Firestore.")
                emit(ResourceUiState.Success(true))
            } else {
                emit(ResourceUiState.Error("Order document not found in Firestore"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Failed updating status to SERVED", e)
            emit(ResourceUiState.Error("Failed to update status: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateOrderStatusToBilling(
        managerId: String,
        floorId: String,
        tableId: String,
        orderDocId: String
    ): Flow<ResourceUiState<Boolean>> = flow {
        Log.d(TAG, "📦 [REPO] Updating Order '$orderDocId' and Table '$tableId' to BILLING...")
        emit(ResourceUiState.Loading)

        try {
            val batch = firestore.batch()

            // 1. Update Order Status
            val orderRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .document(orderDocId)
            
            batch.update(orderRef, AppConstants.FIELD_ORDER_STATUS, ActiveOrderStatus.BILLING.name)

            // 2. Update Table Status to BILLING
            val tableRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)

            batch.update(tableRef, AppConstants.FIELD_STATUS, AppConstants.STATUS_BILLING)

            batch.commit().await()
            Log.i(TAG, "📦 [REPO] Successfully updated Order and Table to BILLING in Firestore.")
            emit(ResourceUiState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Failed updating status to BILLING", e)
            emit(ResourceUiState.Error("Failed to update status: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun updateStatus(
        managerId: String,
        floorId: String,
        tableId: String,
        orderDocId: String,
        newStatus: ActiveOrderStatus
    ): Flow<ResourceUiState<Boolean>> = flow {
        Log.d(TAG, "📦 [REPO] Updating Order '$orderDocId' to ${newStatus.name}...")
        emit(ResourceUiState.Loading)

        try {
            val orderRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)
                .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
                .document(orderDocId)

            orderRef.update(AppConstants.FIELD_ORDER_STATUS, newStatus.name).await()
            Log.i(TAG, "📦 [REPO] Successfully updated order status to ${newStatus.name} in Firestore.")
            emit(ResourceUiState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Failed updating status to ${newStatus.name}", e)
            emit(ResourceUiState.Error("Failed to update order status: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getCashierTokens(managerId: String): List<String> {
        return try {
            val snapshot = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_STAFF)
                .whereIn(AppConstants.FIELD_ROLE, listOf("billing", "cashier", "Billing", "Cashier"))
                .get()
                .await()

            snapshot.documents.mapNotNull { it.getString(AppConstants.FIELD_FCM_TOKEN) }
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Error fetching cashier tokens", e)
            emptyList()
        }
    }
}