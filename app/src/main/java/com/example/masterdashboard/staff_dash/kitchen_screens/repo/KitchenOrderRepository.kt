package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
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

        Log.d(TAG, "📦 [REPO] Opening real-time kitchen stream for manager: $managerId")
        val startTime = System.currentTimeMillis()

        // Listens to all active_orders across table subcollections for this manager
        // IMPORTANT: Composite index (collectionGroup "active_orders" field "restaurantId" ASC, "timestamp" DESC) 
        // is required for this query.
        val listenerRegistration = firestore.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)
            .whereEqualTo(AppConstants.FIELD_RESTAURANT_ID, managerId)
            .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "📦 [REPO] Snapshot error: ${error.message}")
                    // Safely close without propagating a crash-inducing exception
                    // The ViewModel's .catch() or empty check will handle this.
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents ?: emptyList()
                val snapshotTime = System.currentTimeMillis()
                Log.d(TAG, "📦 [REPO] Received snapshot with ${documents.size} docs. Latency: ${snapshotTime - startTime}ms")

                // Process documents in parallel for speed
                launch(Dispatchers.Default) {
                    val ordersList = documents.map { doc ->
                        async {
                            val orderType = (doc.getString(AppConstants.FIELD_ORDER_TYPE) ?: doc.getString("order_type")) ?: AppConstants.ORDER_TYPE_DINE_IN
                            val tableName = doc.getString(AppConstants.FIELD_TABLE_NAME) ?: doc.getString("table_name")

                            // Robust display name logic matching CashierBilling screen
                            val finalDisplayName = when {
                                orderType.contains("TAKE", true) -> "TAKE AWAY"
                                orderType.contains("DELIVERY", true) -> "DELIVERY"
                                !tableName.isNullOrEmpty() -> {
                                    if (tableName!!.startsWith("Table", true)) tableName!! else "Table $tableName"
                                }
                                else -> "Counter Order"
                            }

                            val rawItems = doc.get(AppConstants.FIELD_ORDER_ITEMS) as? List<Map<String, Any>>
                            val items = rawItems?.map { item ->
                                OrderDetailItem(
                                    itemId = item[AppConstants.FIELD_ITEM_ID] as? String ?: "",
                                    itemName = (item[AppConstants.FIELD_ITEM_NAME] as? String ?: item["item_name"] as? String) ?: "",
                                    variantName = item[AppConstants.FIELD_VARIANT_NAME] as? String ?: "",
                                    quantity = (item[AppConstants.FIELD_QUANTITY] as? Number)?.toInt() ?: 0,
                                    orderedQuantity = (item[AppConstants.FIELD_ORDERED_QTY] as? Number)?.toInt() ?: 0,
                                    readyQuantity = (item[AppConstants.FIELD_READY_QTY] as? Number)?.toInt() ?: 0,
                                    itemNote = (item[AppConstants.FIELD_ITEM_NOTE] as? String ?: item["item_note"] as? String) ?: "",
                                    price = (item[AppConstants.FIELD_ITEM_PRICE] as? Number)?.toInt() ?: 0,
                                    rowTotal = (item[AppConstants.FIELD_ROW_TOTAL] as? Number)?.toInt() ?: 0,
                                    category = item[AppConstants.FIELD_CATEGORY] as? String ?: "Veg",
                                    itemStatus = item["itemStatus"] as? String ?: "PENDING"
                                )
                            } ?: emptyList()

                            val rawStatus = doc.getString(AppConstants.FIELD_ORDER_STATUS) ?: doc.getString("order_status") ?: AppConstants.STATUS_PENDING
                            
                            val displayStatus = when {
                                rawStatus.equals(AppConstants.STATUS_PENDING, ignoreCase = true) -> "New"
                                (orderType.contains("TAKE", true) || orderType.contains("DELIVERY", true)) && 
                                        rawStatus.equals(AppConstants.STATUS_PAID, ignoreCase = true) -> "New"
                                rawStatus.equals(AppConstants.STATUS_SERVED, ignoreCase = true) -> "Completed"
                                else -> rawStatus
                            }

                            KitchenOrderDetailData(
                                orderId = doc.getString(AppConstants.FIELD_ORDER_ID) ?: doc.getString("order_id") ?: doc.id,
                                tableName = finalDisplayName,
                                orderStatus = rawStatus,
                                status = displayStatus,
                                specialNotes = doc.getString(AppConstants.FIELD_SPECIAL_NOTES) ?: doc.getString("special_notes") ?: "",
                                rejectionReason = doc.getString(AppConstants.FIELD_REJECTION_REASON) ?: doc.getString("rejection_reason") ?: "",
                                orderType = orderType,
                                restaurantId = doc.getString(AppConstants.FIELD_RESTAURANT_ID) ?: managerId,
                                waiterId = doc.getString(AppConstants.FIELD_WAITER_ID) ?: "",
                                timestamp = doc.getTimestamp(AppConstants.FIELD_TIMESTAMP),
                                items = items,
                                docPath = doc.reference.path
                            )
                        }
                    }.awaitAll()

                    val processTime = System.currentTimeMillis()
                    Log.d(TAG, "📦 [REPO] Finished processing list. Processing Time: ${processTime - snapshotTime}ms")
                    trySend(ordersList)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }
}
