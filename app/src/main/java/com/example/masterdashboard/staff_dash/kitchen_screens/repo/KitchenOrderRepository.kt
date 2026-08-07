package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class KitchenOrderRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        private const val TAG = "KitchenOrderRepo"
    }

    /**
     * Fetches orders in real-time from Firestore using collectionGroup to capture all active orders
     * across different tables and floors for a specific manager.
     */
    fun getRealtimeKitchenOrderDetailDatas(managerId: String): Flow<List<KitchenOrderDetailData>> = callbackFlow {
        if (managerId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Listens to all active_orders across table subcollections for this manager
        val listenerRegistration = firestore.collectionGroup("active_orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents?.filter { it.reference.path.contains("users/$managerId/") } ?: emptyList()

                // Process documents in parallel to match the robust naming logic of CashierBillingRepository
                launch(Dispatchers.IO) {
                    val ordersList = documents.map { doc ->
                        async {
                            val orderType = (doc.getString("orderType") ?: doc.getString("order_type")) ?: "DINE_IN"
                            var tableName = doc.getString("tableName") ?: doc.getString("table_name")

                            // If Dine-In and tableName is missing, try to fetch from parent Table document
                            if (tableName == null && (orderType.equals("DINE_IN", true) || orderType.equals("NORMAL", true))) {
                                try {
                                    val tableDoc = doc.reference.parent.parent?.get()?.await()
                                    tableName = tableDoc?.getString("tableName") ?: tableDoc?.getString("table_name")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to fetch parent table name", e)
                                }
                            }

                            // Robust display name logic matching CashierBilling screen
                            val finalDisplayName = when {
                                orderType.contains("TAKE", true) -> "TAKE AWAY"
                                orderType.contains("DELIVERY", true) -> "DELIVERY"
                                !tableName.isNullOrEmpty() -> {
                                    if (tableName!!.startsWith("Table", true)) tableName!! else "Table $tableName"
                                }
                                else -> "Counter Order"
                            }

                            val rawItems = doc.get("items") as? List<Map<String, Any>>
                            val items = rawItems?.map { item ->
                                OrderDetailItem(
                                    itemId = item["itemId"] as? String ?: "",
                                    itemName = (item["itemName"] as? String ?: item["item_name"] as? String) ?: "",
                                    quantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                                    orderedQuantity = (item["orderedQuantity"] as? Number)?.toInt() ?: 0,
                                    readyQuantity = (item["readyQuantity"] as? Number)?.toInt() ?: 0,
                                    itemNote = (item["itemNote"] as? String ?: item["item_note"] as? String) ?: "",
                                    price = (item["price"] as? Number)?.toInt() ?: 0,
                                    rowTotal = (item["rowTotal"] as? Number)?.toInt() ?: 0,
                                    category = item["category"] as? String ?: "Veg"
                                )
                            } ?: emptyList()

                            val rawStatus = doc.getString("orderStatus") ?: doc.getString("order_status") ?: "PENDING"
                            
                            // Status Mapping: 
                            // 1. PENDING (Dine-in/Waiter) -> New
                            // 2. PAID (Takeaway/Cashier) -> New (if it hasn't been prepared yet)
                            // 3. SERVED (Marked by waiter) -> Completed
                            // 4. Otherwise use the raw status (Preparing, Ready, etc.)
                            val displayStatus = when {
                                rawStatus.equals("PENDING", ignoreCase = true) -> "New"
                                (orderType.contains("TAKE", true) || orderType.contains("DELIVERY", true)) && 
                                        rawStatus.equals("PAID", ignoreCase = true) -> "New"
                                rawStatus.equals("SERVED", ignoreCase = true) -> "Completed"
                                else -> rawStatus
                            }

                            KitchenOrderDetailData(
                                orderId = doc.getString("orderId") ?: doc.getString("order_id") ?: doc.id,
                                tableName = finalDisplayName,
                                orderStatus = rawStatus,
                                status = displayStatus,
                                specialNotes = doc.getString("specialNotes") ?: doc.getString("special_notes") ?: "",
                                rejectionReason = doc.getString("rejectionReason") ?: doc.getString("rejection_reason") ?: "",
                                orderType = orderType,
                                timestamp = doc.getTimestamp("timestamp"),
                                items = items,
                                docPath = doc.reference.path
                            )
                        }
                    }.awaitAll()

                    trySend(ordersList)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}