package com.example.masterdashboard.staff_dash.waiter_screens.order.repo

import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
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
                (doc.id == orderId || doc.getString("orderId") == orderId) &&
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
                            quantity = item.quantity,
                            unitPrice = item.price,
                            totalPrice = if (item.rowTotal > 0) item.rowTotal else item.price * item.quantity,
                            orderedQuantity = item.orderedQuantity
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
                    var tableName = document.getString("tableName") ?: orderModel.tableName
                    if (tableName.isBlank() || tableName == "N/A") {
                        tableName = if (fallbackTableName.isNotBlank()) fallbackTableName else "Table"
                    }

                    // Map status string to enum
                    val statusStr = document.getString("orderStatus") ?: orderModel.orderStatus
                    val status = when (statusStr.uppercase().trim()) {
                        "PENDING" -> ActiveOrderStatus.PENDING
                        "PREPARING" -> ActiveOrderStatus.PREPARING
                        "READY" -> ActiveOrderStatus.READY
                        "SERVED" -> ActiveOrderStatus.SERVED
                        "BILLING" -> ActiveOrderStatus.BILLING
                        "PAID" -> ActiveOrderStatus.PAID
                        else -> ActiveOrderStatus.PENDING
                    }

                    val formattedTime = try {
                        formatter.format(orderModel.timestamp.toDate())
                    } catch (e: Exception) {
                        "Just Now"
                    }

                    val customDisplayOrderId = document.getString("orderId") ?: document.id

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
                        subtotal = orderModel.subtotal.toInt(),
                        gstAmount = orderModel.gst,
                        grandTotal = orderModel.grandTotal
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
    ): Flow<ResourceUiState<Boolean>> = updateStatus(managerId, floorId, tableId, orderDocId, ActiveOrderStatus.SERVED)

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
            
            batch.update(orderRef, "orderStatus", ActiveOrderStatus.BILLING.name)

            // 2. Update Table Status to BILLING
            val tableRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_RES_FLOORS)
                .document(floorId)
                .collection(AppConstants.COLLECTION_TABLES)
                .document(tableId)

            batch.update(tableRef, "status", "BILLING")

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

            orderRef.update("orderStatus", newStatus.name).await()
            Log.i(TAG, "📦 [REPO] Successfully updated order status to ${newStatus.name} in Firestore.")
            emit(ResourceUiState.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "📦 [REPO] Failed updating status to ${newStatus.name}", e)
            emit(ResourceUiState.Error("Failed to update order status: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}