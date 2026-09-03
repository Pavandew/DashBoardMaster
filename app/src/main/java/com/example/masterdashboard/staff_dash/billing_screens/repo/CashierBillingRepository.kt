package com.example.masterdashboard.staff_dash.billing_screens.repo


import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderItemModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class CashierBillingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "CashierBillingRepo"
    }

    /**
     * Streams active orders and completed/paid orders live from Firestore for the Cashier
     */
    fun streamBillingOrders(managerId: String): Flow<List<CashierBillingOrderModel>> = callbackFlow {
        Log.d(TAG, "streamBillingOrders: Fetching orders restricted to Manager: $managerId")

        if (managerId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var activeOrdersList = listOf<CashierBillingOrderModel>()
        var completedOrdersList = listOf<CashierBillingOrderModel>()

        fun emitCombinedList() {
            val combined = (activeOrdersList + completedOrdersList)
                .distinctBy { if (it.orderId.isNotEmpty() && it.orderId != "N/A") it.orderId else it.docPath }
            trySend(combined)
        }

        val targetPrefix = "users/$managerId/"

        // 1. Listen to active_orders across table subcollections
        val activeRegistration = firestore.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)
            .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching active billing orders snapshot", error)
                    return@addSnapshotListener
                }

                val allDocs = snapshot?.documents ?: emptyList()
                val documents = allDocs.filter { it.reference.path.contains(targetPrefix) }

                launch(Dispatchers.IO) {
                    activeOrdersList = documents.map { doc ->
                        async { parseOrderDocument(doc) }
                    }.awaitAll()

                    emitCombinedList()
                }
            }

        // 2. Listen to completed_orders collection for this manager
        val completedRegistration = firestore.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
            .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching completed billing orders snapshot", error)
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents ?: emptyList()

                launch(Dispatchers.IO) {
                    completedOrdersList = documents.map { doc ->
                        async { parseOrderDocument(doc) }
                    }.awaitAll()

                    emitCombinedList()
                }
            }

        awaitClose {
            Log.d(TAG, "Dismantling billing orders snapshot listeners")
            activeRegistration.remove()
            completedRegistration.remove()
        }
    }

    private suspend fun parseOrderDocument(doc: com.google.firebase.firestore.DocumentSnapshot): CashierBillingOrderModel {
        return try {
            val orderId = doc.getString(AppConstants.FIELD_ORDER_ID) ?: doc.id
            val tableId = doc.getString(AppConstants.FIELD_TABLE_ID) ?: ""
            val orderType = doc.getString(AppConstants.FIELD_ORDER_TYPE)?.ifEmpty { AppConstants.ORDER_TYPE_DINE_IN } ?: AppConstants.ORDER_TYPE_DINE_IN

            var tableName = doc.getString(AppConstants.FIELD_TABLE_NAME)?.ifEmpty { null }

            if (tableName == null && orderType == AppConstants.ORDER_TYPE_DINE_IN) {
                try {
                    val tableDoc = doc.reference.parent.parent?.get()?.await()
                    tableName = tableDoc?.getString(AppConstants.FIELD_TABLE_NAME)?.ifEmpty { null }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch parent table name for path: ${doc.reference.path}", e)
                }
            }

            val finalDisplayName = when {
                orderType == AppConstants.ORDER_TYPE_TAKE_AWAY -> "TAKE AWAY"
                orderType == AppConstants.ORDER_TYPE_DELIVERY -> "DELIVERY"
                !tableName.isNullOrEmpty() -> {
                    if (tableName.startsWith("Table", true)) tableName else "Table $tableName"
                }
                else -> "Counter Order"
            }

            val orderStatus = doc.getString(AppConstants.FIELD_ORDER_STATUS) ?: AppConstants.STATUS_SERVED
            val subtotal = (doc.get(AppConstants.FIELD_SUBTOTAL) as? Number)?.toDouble() ?: 0.0
            val taxAmount = (doc.get(AppConstants.FIELD_GST) as? Number)?.toDouble() ?: 0.0
            val grandTotal = (doc.get(AppConstants.FIELD_GRAND_TOTAL) as? Number)?.toDouble() ?: 0.0
            val discountAmount = (doc.get(AppConstants.FIELD_DISCOUNT_AMOUNT) as? Number)?.toDouble() ?: 0.0
            val timestamp = doc.getTimestamp(AppConstants.FIELD_TIMESTAMP) ?: Timestamp.now()
            val paidAt = doc.getTimestamp(AppConstants.FIELD_PAID_AT)
            val paymentMethod = doc.getString(AppConstants.FIELD_PAYMENT_METHOD) ?: ""
            val waiterId = doc.getString(AppConstants.FIELD_WAITER_ID) ?: ""
            val customerName = doc.getString(AppConstants.FIELD_CUSTOMER_NAME) ?: ""
            val customerPhone = doc.getString(AppConstants.FIELD_CUSTOMER_MOBILE) ?: ""

            val rawItems = doc.get(AppConstants.FIELD_ORDER_ITEMS) as? List<Map<String, Any>>
            val itemsList = rawItems?.map { item ->
                OrderItemModel(
                    itemId = item[AppConstants.FIELD_ITEM_ID] as? String ?: "",
                    itemName = item[AppConstants.FIELD_ITEM_NAME] as? String ?: "Item",
                    variantName = item[AppConstants.FIELD_VARIANT_NAME] as? String ?: "",
                    price = (item[AppConstants.FIELD_ITEM_PRICE] as? Number)?.toInt() ?: 0,
                    quantity = (item[AppConstants.FIELD_QUANTITY] as? Number)?.toInt() ?: 1,
                    rowTotal = (item[AppConstants.FIELD_ROW_TOTAL] as? Number)?.toInt() ?: 0,
                    orderedQuantity = (item[AppConstants.FIELD_ORDERED_QTY] as? Number)?.toInt() ?: 0
                )
            } ?: emptyList()

            val summaryStr = itemsList.joinToString(", ") { "${it.quantity}x ${it.itemName}" }

            CashierBillingOrderModel(
                orderId = orderId,
                tableId = tableId,
                tableName = finalDisplayName,
                orderType = orderType,
                orderStatus = orderStatus,
                itemsSummary = summaryStr,
                items = itemsList,
                subtotal = subtotal,
                taxAmount = taxAmount,
                discountAmount = discountAmount,
                grandTotal = grandTotal,
                timestamp = timestamp,
                paidAt = paidAt,
                paymentMethod = paymentMethod,
                customerName = customerName,
                customerPhone = customerPhone,
                waiterId = waiterId,
                docPath = doc.reference.path
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing order document: ${doc.id}", e)
            CashierBillingOrderModel(
                orderId = doc.id,
                tableName = "Order",
                orderStatus = AppConstants.STATUS_SERVED,
                docPath = doc.reference.path
            )
        }
    }


    /**
     * Fetches a single order's full details using its Firestore document path.
     */
    fun fetchOrderDetails(docPath: String): Flow<ResourceUiState<CashierBillingOrderModel>> = flow {
        emit(ResourceUiState.Loading)
        try {
            var doc = firestore.document(docPath).get().await()
            if (!doc.exists()) {
                // If deleted from active_orders, check completed_orders
                val pathSegments = docPath.split("/")
                if (pathSegments.size >= 2 && pathSegments[0] == "users") {
                    val managerId = pathSegments[1]
                    val orderId = docPath.substringAfterLast("/")
                    val completedRef = firestore.collection(AppConstants.COLLECTION_USERS)
                        .document(managerId)
                        .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
                        .document(orderId)
                    val completedDoc = completedRef.get().await()
                    if (completedDoc.exists()) {
                        doc = completedDoc
                    }
                }
            }

            if (doc.exists()) {
                val model = parseOrderDocument(doc)
                emit(ResourceUiState.Success(model))
            } else {
                emit(ResourceUiState.Error("Order not found"))
            }
        } catch (e: Exception) {
            emit(ResourceUiState.Error(e.message ?: "Fetch failed"))
        }
    }

    /**
     * Completes the order by updating its status to PAID, recording payment details,
     * and moving it to a central completed_orders collection for monthly reporting.
     */
    fun settleOrder(
        order: CashierBillingOrderModel,
        paymentMode: String,
        discount: Double
    ): Flow<ResourceUiState<String>> = callbackFlow {
        trySend(ResourceUiState.Loading)

        if (order.docPath.isEmpty()) {
            trySend(ResourceUiState.Error("Invalid order path"))
            close()
            return@callbackFlow
        }

        val batch = firestore.batch()
        val orderRef = firestore.document(order.docPath)
        
        // 1. Prepare Payment & Reporting Data
        val currentTime = Timestamp.now()
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthYear = sdfMonth.format(currentTime.toDate())
        val exactDate = sdfDate.format(currentTime.toDate())

        val finalGrandTotal = (order.subtotal + order.taxAmount - discount)

        val fullCompletedOrderData = mapOf(
            AppConstants.FIELD_ORDER_ID to order.orderId,
            AppConstants.FIELD_TABLE_ID to order.tableId,
            AppConstants.FIELD_TABLE_NAME to order.tableName,
            AppConstants.FIELD_CUSTOMER_NAME to order.customerName,
            AppConstants.FIELD_CUSTOMER_MOBILE to order.customerPhone,
            AppConstants.FIELD_ORDER_TYPE to order.orderType,
            AppConstants.FIELD_ORDER_STATUS to AppConstants.STATUS_PAID,
            AppConstants.FIELD_ORDER_ITEMS to order.items.map { item ->
                mapOf(
                    AppConstants.FIELD_ITEM_ID to item.itemId,
                    AppConstants.FIELD_ITEM_NAME to item.itemName,
                    AppConstants.FIELD_VARIANT_NAME to item.variantName,
                    AppConstants.FIELD_ITEM_PRICE to item.price,
                    AppConstants.FIELD_QUANTITY to item.quantity,
                    AppConstants.FIELD_ROW_TOTAL to item.rowTotal,
                    AppConstants.FIELD_ORDERED_QTY to item.orderedQuantity
                )
            },
            AppConstants.FIELD_SUBTOTAL to order.subtotal,
            AppConstants.FIELD_GST to order.taxAmount,
            AppConstants.FIELD_DISCOUNT_AMOUNT to discount,
            AppConstants.FIELD_GRAND_TOTAL to finalGrandTotal,
            AppConstants.FIELD_TIMESTAMP to order.timestamp,
            AppConstants.FIELD_PAID_AT to currentTime,
            AppConstants.FIELD_PAYMENT_METHOD to paymentMode,
            AppConstants.FIELD_WAITER_ID to order.waiterId,
            AppConstants.FIELD_BILLING_MONTH to monthYear,
            AppConstants.FIELD_BILLING_DATE to exactDate
        )

        // 2. Store full copy in a central 'completed_orders' collection
        val pathSegments = order.docPath.split("/")
        if (pathSegments.size >= 2 && pathSegments[0] == "users") {
            val managerId = pathSegments[1]
            val completedOrderRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
                .document(order.orderId)
            
            batch.set(completedOrderRef, fullCompletedOrderData, com.google.firebase.firestore.SetOptions.merge())
        }

        // 3. Reset the table status to FREE
        if (pathSegments.contains(AppConstants.COLLECTION_TABLES) && pathSegments.size >= 6) {
            try {
                val tableRef = firestore.collection(pathSegments[0])
                    .document(pathSegments[1])
                    .collection(pathSegments[2])
                    .document(pathSegments[3])
                    .collection(pathSegments[4])
                    .document(pathSegments[5])

                Log.d(TAG, "settleOrder: Releasing table. Path: ${tableRef.path}")
                
                // Use set with merge instead of update so it won't fail with NOT_FOUND if table doc doesn't exist
                batch.set(tableRef, mapOf(
                    AppConstants.FIELD_STATUS to AppConstants.STATUS_FREE,
                    AppConstants.FIELD_CUSTOMER_NAME_TABLE to "",
                    AppConstants.FIELD_CURRENT_BILL to 0.0
                ), com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.e(TAG, "settleOrder: Error reconstructing table path", e)
            }
        }

        // 4. Delete the active order document
        batch.delete(orderRef)


        // 5. Update CRM (Customer Relationship Management)
        if (order.customerPhone.isNotEmpty()) {
            if (pathSegments.size >= 2 && pathSegments[0] == "users") {
                val managerId = pathSegments[1]
                val customerRef = firestore.collection(AppConstants.COLLECTION_USERS)
                    .document(managerId)
                    .collection(AppConstants.COLLECTION_CUSTOMERS)
                    .document(order.customerPhone)
                
                val customerData = mapOf(
                    "customerId" to order.customerPhone,
                    "customerName" to order.customerName,
                    "customerMobile" to order.customerPhone,
                    "lastVisit" to currentTime,
                    "visitCount" to com.google.firebase.firestore.FieldValue.increment(1),
                    "totalSpent" to com.google.firebase.firestore.FieldValue.increment(finalGrandTotal)
                )
                batch.set(customerRef, customerData, com.google.firebase.firestore.SetOptions.merge())
            }
        }

        batch.commit()
            .addOnSuccessListener {
                Log.i(TAG, "💰 [REPO] Order ${order.orderId} settled. Revenue: ₹$finalGrandTotal. Reporting indexed for $monthYear.")
                trySend(ResourceUiState.Success("Bill Settled Successfully"))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ [REPO] Settlement failed for order ${order.orderId}", e)
                trySend(ResourceUiState.Error(e.message ?: "Update failed"))
                close(e)
            }

        awaitClose { }
    }

    /**
     * Marks a Takeaway/Delivery order as COMPLETED after the customer picks it up.
     */
    fun confirmTakeawayPickup(order: CashierBillingOrderModel): Flow<ResourceUiState<String>> = callbackFlow {
        trySend(ResourceUiState.Loading)

        if (order.docPath.isEmpty()) {
            trySend(ResourceUiState.Error("Invalid order path"))
            close()
            return@callbackFlow
        }

        val orderRef = firestore.document(order.docPath)
        orderRef.set(mapOf(AppConstants.FIELD_ORDER_STATUS to AppConstants.STATUS_COMPLETED), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.i(TAG, "📦 [REPO] Takeaway order ${order.orderId} marked as COMPLETED (Handed Over).")
                trySend(ResourceUiState.Success("Order Handed Over"))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ [REPO] Failed to mark order ${order.orderId} as completed", e)
                trySend(ResourceUiState.Error(e.message ?: "Update failed"))
                close(e)
            }

        awaitClose { }
    }
}
