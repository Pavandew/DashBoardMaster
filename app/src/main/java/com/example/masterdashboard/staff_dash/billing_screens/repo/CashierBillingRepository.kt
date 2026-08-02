package com.example.masterdashboard.staff_dash.billing_screens.repo


import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
        val registration = firestore.collectionGroup("active_orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
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
                            val orderId = doc.getString("orderId") ?: doc.id
                            val tableId = doc.getString("tableId") ?: ""
                            val orderType = doc.getString("orderType")?.ifEmpty { "DINE_IN" } ?: "DINE_IN"
                            
                            // 1. Try to get tableName from the order document itself first
                            var tableName = doc.getString("tableName")?.ifEmpty { null }
                            
                            // 2. CRITICAL: If empty it's a Dine-In order, fetch the parent Table document
                            if (tableName == null && orderType == "DINE_IN") {
                                try {
                                    val tableDoc = doc.reference.parent.parent?.get()?.await()
                                    tableName = tableDoc?.getString("tableName")?.ifEmpty { null }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to fetch parent table name for path: ${doc.reference.path}", e)
                                }
                            }
                            
                            // 3. Robust Display Name Selection
                            val finalDisplayName = when {
                                orderType == "TAKE_AWAY" -> "TAKE AWAY"
                                orderType == "DELIVERY" -> "DELIVERY"
                                !tableName.isNullOrEmpty() -> {
                                    if (tableName!!.startsWith("Table", true)) tableName!! else "Table $tableName"
                                }
                                else -> "Counter Order"
                            }

                            val orderStatus = doc.getString("orderStatus") ?: "SERVED"
                            val subtotal = doc.getDouble("subtotal") ?: 0.0
                            val taxAmount = doc.getDouble("gst") ?: 0.0
                            val grandTotal = doc.getDouble("grandTotal") ?: 0.0
                            val timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                            val paidAt = doc.getTimestamp("paidAt")
                            val paymentMethod = doc.getString("paymentMethod") ?: doc.getString("paymentMode") ?: ""

                            // Convert Firestore list to OrderItemModel list
                            val rawItems = doc.get("items") as? List<Map<String, Any>>
                            val itemsList = rawItems?.map { item ->
                                com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderItemModel(
                                    itemId = item["itemId"] as? String ?: "",
                                    itemName = item["itemName"] as? String ?: "Item",
                                    price = (item["price"] as? Number)?.toInt() ?: 0,
                                    quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                                    rowTotal = (item["rowTotal"] as? Number)?.toInt() ?: 0,
                                    orderedQuantity = (item["orderedQuantity"] as? Number)?.toInt() ?: 0
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
     * Completes the order by updating its status to PAID, recording payment details,
     * and moving it to a central completed_orders collection for monthly reporting.
     */
    fun settleOrder(
        order: CashierBillingOrderModel,
        paymentMode: String,
        discount: Double
    ): Flow<com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState<String>> = callbackFlow {
        trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Loading)

        if (order.docPath.isEmpty()) {
            trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Error("Invalid order path"))
            close()
            return@callbackFlow
        }

        val batch = firestore.batch()
        val orderRef = firestore.document(order.docPath)
        
        // 1. Prepare Payment & Reporting Data
        val currentTime = Timestamp.now()
        val sdfMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val monthYear = sdfMonth.format(currentTime.toDate())
        val exactDate = sdfDate.format(currentTime.toDate())

        val finalGrandTotal = (order.subtotal + order.taxAmount - discount)

        val orderUpdates = mapOf(
            "orderStatus" to "PAID",
            "paymentMethod" to paymentMode,
            "discountAmount" to discount,
            "grandTotal" to finalGrandTotal,
            "paidAt" to currentTime,
            "billingMonth" to monthYear, // Helpful for monthly revenue reports
            "billingDate" to exactDate    // Helpful for daily revenue reports
        )

        // 2. Update the original order document
        batch.update(orderRef, orderUpdates)

        // 3. Store a copy in a central 'completed_orders' collection for the Manager/Owner
        // Extract managerId from the path (format: users/{managerId}/...)
        val pathSegments = order.docPath.split("/")
        if (pathSegments.size >= 2 && pathSegments[0] == "users") {
            val managerId = pathSegments[1]
            val completedOrderRef = firestore.collection("users")
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
            batch.update(tableRef, "status", "FREE")
        }

        batch.commit()
            .addOnSuccessListener {
                Log.i(TAG, "💰 [REPO] Order ${order.orderId} settled. Revenue: ₹$finalGrandTotal. Reporting indexed for $monthYear.")
                trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Success("Bill Settled Successfully"))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ [REPO] Settlement failed for order ${order.orderId}", e)
                trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Error(e.message ?: "Update failed"))
                close(e)
            }

        awaitClose { }
    }

    /**
     * Marks a Takeaway/Delivery order as COMPLETED after the customer picks it up.
     */
    fun confirmTakeawayPickup(order: CashierBillingOrderModel): Flow<com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState<String>> = callbackFlow {
        trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Loading)

        if (order.docPath.isEmpty()) {
            trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Error("Invalid order path"))
            close()
            return@callbackFlow
        }

        val orderRef = firestore.document(order.docPath)
        orderRef.update("orderStatus", "COMPLETED")
            .addOnSuccessListener {
                Log.i(TAG, "📦 [REPO] Takeaway order ${order.orderId} marked as COMPLETED (Handed Over).")
                trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Success("Order Handed Over"))
                close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ [REPO] Failed to mark order ${order.orderId} as completed", e)
                trySend(com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Error(e.message ?: "Update failed"))
                close(e)
            }

        awaitClose { }
    }
}