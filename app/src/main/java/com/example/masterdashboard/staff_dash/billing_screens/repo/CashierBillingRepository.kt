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
     * Streams active orders live from Firestore across all active floor tables
     */
    fun streamBillingOrders(managerId: String): Flow<List<CashierBillingOrderModel>> = callbackFlow {
        Log.d(TAG, "streamBillingOrders: Fetching orders restricted to Manager: $managerId")

        if (managerId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Listens to all active_orders across table subcollections
        val registration = firestore.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)
            .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching billing orders snapshot", error)
                    close(error)
                    return@addSnapshotListener
                }

                val allDocs = snapshot?.documents ?: emptyList()
                
                // Use path-based filtering to ensure we capture all orders (old and new)
                // for this specific manager without needing a composite index or the new 'restaurantId' field.
                val targetPrefix = "users/$managerId/"
                val documents = allDocs.filter { it.reference.path.contains(targetPrefix) }
                
                Log.d(TAG, "streamBillingOrders: Found ${documents.size} valid orders for Manager: $managerId")

                // Process documents in parallel to fetch parent table names if missing in the child doc
                launch(Dispatchers.IO) {
                    val ordersList = documents.map { doc ->
                        async {
                            val orderId = doc.getString(AppConstants.FIELD_ORDER_ID) ?: doc.id
                            val tableId = doc.getString(AppConstants.FIELD_TABLE_ID) ?: ""
                            val orderType = doc.getString(AppConstants.FIELD_ORDER_TYPE)?.ifEmpty { AppConstants.ORDER_TYPE_DINE_IN } ?: AppConstants.ORDER_TYPE_DINE_IN
                            
                            // 1. Try to get tableName from the order document itself first
                            var tableName = doc.getString(AppConstants.FIELD_TABLE_NAME)?.ifEmpty { null }
                            
                            // 2. CRITICAL: If empty it's a Dine-In order, fetch the parent Table document
                            if (tableName == null && orderType == AppConstants.ORDER_TYPE_DINE_IN) {
                                try {
                                    val tableDoc = doc.reference.parent.parent?.get()?.await()
                                    tableName = tableDoc?.getString(AppConstants.FIELD_TABLE_NAME)?.ifEmpty { null }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to fetch parent table name for path: ${doc.reference.path}", e)
                                }
                            }
                            
                            // 3. Robust Display Name Selection
                            val finalDisplayName = when {
                                orderType == AppConstants.ORDER_TYPE_TAKE_AWAY -> "TAKE AWAY"
                                orderType == AppConstants.ORDER_TYPE_DELIVERY -> "DELIVERY"
                                !tableName.isNullOrEmpty() -> {
                                    if (tableName!!.startsWith("Table", true)) tableName!! else "Table $tableName"
                                }
                                else -> "Counter Order"
                            }

                            val orderStatus = doc.getString(AppConstants.FIELD_ORDER_STATUS) ?: AppConstants.STATUS_SERVED
                            val subtotal = doc.getDouble(AppConstants.FIELD_SUBTOTAL) ?: 0.0
                            val taxAmount = doc.getDouble(AppConstants.FIELD_GST) ?: 0.0
                            val grandTotal = doc.getDouble(AppConstants.FIELD_GRAND_TOTAL) ?: 0.0
                            val timestamp = doc.getTimestamp(AppConstants.FIELD_TIMESTAMP) ?: Timestamp.now()
                            val paidAt = doc.getTimestamp(AppConstants.FIELD_PAID_AT)
                            val paymentMethod = doc.getString(AppConstants.FIELD_PAYMENT_METHOD) ?: doc.getString("paymentMode") ?: ""
                            val waiterId = doc.getString(AppConstants.FIELD_WAITER_ID) ?: ""
                            val customerName = doc.getString(AppConstants.FIELD_CUSTOMER_NAME) ?: ""
                            val customerPhone = doc.getString(AppConstants.FIELD_CUSTOMER_MOBILE) ?: ""

                            // Convert Firestore list to OrderItemModel list
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
                                grandTotal = grandTotal,
                                timestamp = timestamp,
                                paidAt = paidAt,
                                paymentMethod = paymentMethod,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                waiterId = waiterId,
                                docPath = doc.reference.path
                            )
                        }
                    }.awaitAll()

                    Log.i(TAG, "📊 [REPO] Successfully fetched ${ordersList.size} billing orders with proper table names.")
                    trySend(ordersList)
                }
            }

        awaitClose {
            Log.d(TAG, "Dismantling billing orders snapshot listener")
            registration.remove()
        }
    }

    /**
     * Fetches a single order's full details using its Firestore document path.
     */
    fun fetchOrderDetails(docPath: String): Flow<ResourceUiState<CashierBillingOrderModel>> = flow {
        emit(ResourceUiState.Loading)
        try {
            val doc = firestore.document(docPath).get().await()
            if (doc.exists()) {
                val orderId = doc.getString(AppConstants.FIELD_ORDER_ID) ?: doc.id
                val tableId = doc.getString(AppConstants.FIELD_TABLE_ID) ?: ""
                val orderType = doc.getString(AppConstants.FIELD_ORDER_TYPE) ?: AppConstants.ORDER_TYPE_DINE_IN
                
                var tableName = doc.getString(AppConstants.FIELD_TABLE_NAME)
                if (tableName.isNullOrEmpty() && orderType == AppConstants.ORDER_TYPE_DINE_IN) {
                    val tableDoc = doc.reference.parent.parent?.get()?.await()
                    tableName = tableDoc?.getString(AppConstants.FIELD_TABLE_NAME)
                }

                val finalDisplayName = when {
                    orderType == AppConstants.ORDER_TYPE_TAKE_AWAY -> "TAKE AWAY"
                    orderType == AppConstants.ORDER_TYPE_DELIVERY -> "DELIVERY"
                    !tableName.isNullOrEmpty() -> if (tableName.startsWith("Table", true)) tableName else "Table $tableName"
                    else -> "Counter Order"
                }

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

                val model = CashierBillingOrderModel(
                    orderId = orderId,
                    tableId = tableId,
                    tableName = finalDisplayName,
                    orderType = orderType,
                    orderStatus = doc.getString(AppConstants.FIELD_ORDER_STATUS) ?: AppConstants.STATUS_SERVED,
                    items = itemsList,
                    subtotal = doc.getDouble(AppConstants.FIELD_SUBTOTAL) ?: 0.0,
                    taxAmount = doc.getDouble(AppConstants.FIELD_GST) ?: 0.0,
                    grandTotal = doc.getDouble(AppConstants.FIELD_GRAND_TOTAL) ?: 0.0,
                    timestamp = doc.getTimestamp(AppConstants.FIELD_TIMESTAMP) ?: Timestamp.now(),
                    docPath = doc.reference.path
                )
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

        val orderUpdates = mapOf(
            AppConstants.FIELD_ORDER_STATUS to AppConstants.STATUS_PAID,
            AppConstants.FIELD_PAYMENT_METHOD to paymentMode,
            AppConstants.FIELD_DISCOUNT_AMOUNT to discount,
            AppConstants.FIELD_GRAND_TOTAL to finalGrandTotal,
            AppConstants.FIELD_PAID_AT to currentTime,
            AppConstants.FIELD_BILLING_MONTH to monthYear, // Helpful for monthly revenue reports
            AppConstants.FIELD_BILLING_DATE to exactDate    // Helpful for daily revenue reports
        )

        // 2. Update the original order document
        batch.update(orderRef, orderUpdates)

        // 3. Store a copy in a central 'completed_orders' collection for the Manager/Owner
        // Extract managerId from the path (format: users/{managerId}/...)
        val pathSegments = order.docPath.split("/")
        if (pathSegments.size >= 2 && pathSegments[0] == "users") {
            val managerId = pathSegments[1]
            val completedOrderRef = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
                .document(order.orderId)
            
            // We use set with merge to ensure all item details are copied over
            // but we add the new payment fields too
            batch.set(completedOrderRef, orderUpdates, com.google.firebase.firestore.SetOptions.merge())
        }

        // 4. Reset the table status to FREE so waiters can take new guests
        val tableRef = orderRef.parent.parent
        if (tableRef != null) {
            batch.update(tableRef, AppConstants.FIELD_STATUS, AppConstants.STATUS_FREE)
            // Also clear customer name from table
            batch.update(tableRef, AppConstants.FIELD_CUSTOMER_NAME_TABLE, null)
        }

        // 5. Update CRM (Customer Relationship Management)
        if (order.customerPhone.isNotEmpty()) {
            val pathSegments = order.docPath.split("/")
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
        orderRef.update(AppConstants.FIELD_ORDER_STATUS, AppConstants.STATUS_COMPLETED)
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
