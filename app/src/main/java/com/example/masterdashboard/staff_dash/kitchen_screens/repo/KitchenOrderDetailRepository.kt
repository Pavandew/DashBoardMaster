package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
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
                    val orderType = (snapshot.getString(AppConstants.FIELD_ORDER_TYPE) ?: snapshot.getString("order_type")) ?: AppConstants.ORDER_TYPE_DINE_IN
                    val rawStatus = (snapshot.getString(AppConstants.FIELD_ORDER_STATUS) ?: snapshot.getString("order_status")) ?: AppConstants.STATUS_PENDING
                    
                    // Status Mapping for UI
                    val displayStatus = when {
                        rawStatus.equals(AppConstants.STATUS_PENDING, ignoreCase = true) -> "New"
                        (orderType.contains("TAKE", true) || orderType.contains("DELIVERY", true)) && 
                                rawStatus.equals(AppConstants.STATUS_PAID, ignoreCase = true) -> "New"
                        else -> rawStatus
                    }

                    // Manual extraction of the items list
                    val rawItems = snapshot.get(AppConstants.FIELD_ORDER_ITEMS) as? List<Map<String, Any>>
                    val itemsList = rawItems?.map { item ->
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

                    val orderData = KitchenOrderDetailData(
                        orderId = snapshot.getString(AppConstants.FIELD_ORDER_ID) ?: snapshot.getString("order_id") ?: snapshot.id,
                        docPath = snapshot.reference.path,
                        tableName = (snapshot.getString(AppConstants.FIELD_TABLE_NAME) ?: snapshot.getString("table_name")) ?: "Counter",
                        orderStatus = rawStatus,
                        status = displayStatus,
                        specialNotes = (snapshot.getString(AppConstants.FIELD_SPECIAL_NOTES) ?: snapshot.getString("special_notes")) ?: "",
                        orderType = orderType,
                        timestamp = snapshot.getTimestamp(AppConstants.FIELD_TIMESTAMP),
                        items = itemsList,
                        customerName = snapshot.getString(AppConstants.FIELD_CUSTOMER_NAME) ?: "",
                        customerPhone = snapshot.getString(AppConstants.FIELD_CUSTOMER_MOBILE) ?: "",
                        waiterId = snapshot.getString(AppConstants.FIELD_WAITER_ID) ?: "", // FIXED: Added missing waiterId mapping
                        subtotal = snapshot.getDouble(AppConstants.FIELD_SUBTOTAL) ?: 0.0,
                        gst = (snapshot.getDouble(AppConstants.FIELD_GST) ?: snapshot.getDouble(AppConstants.FIELD_TAX)) ?: 0.0,
                        grandTotal = snapshot.getDouble(AppConstants.FIELD_GRAND_TOTAL) ?: 0.0,
                        paymentMethod = snapshot.getString(AppConstants.FIELD_PAYMENT_METHOD) ?: ""
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
            val firestoreStatus = if (newStatus.equals("New", ignoreCase = true)) AppConstants.STATUS_PENDING else newStatus
            val updates = mutableMapOf<String, Any>(AppConstants.FIELD_ORDER_STATUS to firestoreStatus)
            if (reason.isNotEmpty()) updates[AppConstants.FIELD_REJECTION_REASON] = reason
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
                AppConstants.FIELD_ORDER_ITEMS to updatedItems,
                AppConstants.FIELD_SUBTOTAL to newSubtotal,
                AppConstants.FIELD_GST to newGst,
                AppConstants.FIELD_GRAND_TOTAL to newGrandTotal,
                AppConstants.FIELD_REJECTION_REASON to rejectionReason
            )
            firestore.document(docPath).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "updateOrderWithRejectedItems failure", e)
            throw e
        }
    }
}