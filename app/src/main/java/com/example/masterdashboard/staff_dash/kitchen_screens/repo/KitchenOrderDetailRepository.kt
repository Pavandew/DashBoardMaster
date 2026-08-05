package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class KitchenOrderDetailsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "KitchenDetailsRepo"
    }

    /**
     * Listens to a single order's details in real-time.
     * Uses manual mapping to ensure data integrity regardless of Firestore value types.
     */
    fun getLiveOrderDetails(docPath: String): Flow<KitchenOrderDetailData?> = callbackFlow {
        if (docPath.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = firestore.document(docPath)
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore snapshot failure", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    // Manual extraction of top-level fields
                    val orderType = (snapshot.getString("orderType") ?: snapshot.getString("order_type")) ?: "DINE_IN"
                    val rawStatus = (snapshot.getString("orderStatus") ?: snapshot.getString("order_status")) ?: "PENDING"
                    
                    // Status Mapping for UI
                    val displayStatus = when {
                        rawStatus.equals("PENDING", ignoreCase = true) -> "New"
                        (orderType.contains("TAKE", true) || orderType.contains("DELIVERY", true)) && 
                                rawStatus.equals("PAID", ignoreCase = true) -> "New"
                        else -> rawStatus
                    }

                    // Manual extraction of the items list
                    val rawItems = snapshot.get("items") as? List<Map<String, Any>>
                    val itemsList = rawItems?.map { item ->
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

                    val orderData = KitchenOrderDetailData(
                        orderId = snapshot.id,
                        docPath = snapshot.reference.path,
                        tableName = (snapshot.getString("tableName") ?: snapshot.getString("table_name")) ?: "Counter",
                        orderStatus = rawStatus,
                        status = displayStatus,
                        specialNotes = (snapshot.getString("specialNotes") ?: snapshot.getString("special_notes")) ?: "",
                        orderType = orderType,
                        timestamp = snapshot.getTimestamp("timestamp"),
                        items = itemsList,
                        customerName = snapshot.getString("customerName") ?: "",
                        customerPhone = snapshot.getString("customerPhone") ?: "",
                        subtotal = snapshot.getDouble("subtotal") ?: 0.0,
                        gst = (snapshot.getDouble("gst") ?: snapshot.getDouble("tax")) ?: 0.0,
                        grandTotal = snapshot.getDouble("grandTotal") ?: 0.0,
                        paymentMethod = snapshot.getString("paymentMethod") ?: ""
                    )
                    
                    trySend(orderData)
                } catch (e: Exception) {
                    Log.e(TAG, "Manual mapping failed for order details", e)
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose { listenerRegistration.remove() }
    }

    /**
     * Updates the status of an order in Firestore.
     */
    suspend fun updateOrderStatus(docPath: String, newStatus: String, reason: String = "") {
        if (docPath.isEmpty()) return
        try {
            val firestoreStatus = if (newStatus.equals("New", ignoreCase = true)) "PENDING" else newStatus
            val updates = mutableMapOf<String, Any>("orderStatus" to firestoreStatus)
            if (reason.isNotEmpty()) updates["rejectionReason"] = reason
            firestore.document(docPath).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "updateOrderStatus failure", e)
            throw e
        }
    }

    /**
     * Updates the order by removing specific items and updating totals.
     */
    suspend fun updateOrderWithRejectedItems(
        docPath: String,
        updatedItems: List<Map<String, Any>>,
        newSubtotal: Double,
        newGst: Double,
        newGrandTotal: Double,
        rejectionReason: String
    ) {
        if (docPath.isEmpty()) return
        try {
            val updates = mapOf(
                "items" to updatedItems,
                "subtotal" to newSubtotal,
                "gst" to newGst,
                "grandTotal" to newGrandTotal,
                "rejectionReason" to rejectionReason
            )
            firestore.document(docPath).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "updateOrderWithRejectedItems failure", e)
            throw e
        }
    }
}