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

class KitchenPreparationDetailRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "KitchenWorkstationRepo"
    }

    /**
     * Streams real-time ticket modifications for a specific document using its full path.
     */
    fun listenToOrderDetails(docPath: String): Flow<KitchenOrderDetailData?> = callbackFlow {
        if (docPath.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        Log.d(TAG, "listenToOrderDetails: Registering Firestore snapshot channel on path: $docPath")

        val docRef = firestore.document(docPath)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore snapshot collection dropped for path: $docPath", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    // Manual extraction to ensure field parity with other screens
                    val orderId = snapshot.getString(AppConstants.FIELD_ORDER_ID) 
                        ?: snapshot.getString("order_id") 
                        ?: snapshot.id

                    val tableName = snapshot.getString(AppConstants.FIELD_TABLE_NAME) 
                        ?: snapshot.getString("table_name") 
                        ?: "Table"
                        
                    val rawStatus = snapshot.getString(AppConstants.FIELD_ORDER_STATUS) 
                        ?: snapshot.getString("order_status") 
                        ?: "New"

                    val orderType = snapshot.getString(AppConstants.FIELD_ORDER_TYPE) 
                        ?: AppConstants.ORDER_TYPE_DINE_IN

                    // Manual extraction of items list with support for variants
                    val rawItems = snapshot.get(AppConstants.FIELD_ORDER_ITEMS) as? List<Map<String, Any>>
                    val itemsList = rawItems?.map { item ->
                        OrderDetailItem(
                            itemId = item[AppConstants.FIELD_ITEM_ID] as? String ?: "",
                            itemName = (item[AppConstants.FIELD_ITEM_NAME] as? String
                                ?: item["item_name"] as? String) ?: "",
                            variantName = item[AppConstants.FIELD_VARIANT_NAME] as? String ?: "",
                            quantity = (item[AppConstants.FIELD_QUANTITY] as? Number)?.toInt() ?: 0,
                            orderedQuantity = (item[AppConstants.FIELD_ORDERED_QTY] as? Number)?.toInt()
                                ?: 0,
                            readyQuantity = (item[AppConstants.FIELD_READY_QTY] as? Number)?.toInt()
                                ?: 0,
                            itemNote = (item[AppConstants.FIELD_ITEM_NOTE] as? String
                                ?: item["item_note"] as? String) ?: "",
                            price = (item[AppConstants.FIELD_ITEM_PRICE] as? Number)?.toInt() ?: 0,
                            rowTotal = (item[AppConstants.FIELD_ROW_TOTAL] as? Number)?.toInt()
                                ?: 0,
                            category = item[AppConstants.FIELD_CATEGORY] as? String ?: "Veg",
                            itemStatus = item["itemStatus"] as? String ?: "PENDING"
                        )
                    } ?: emptyList()

                    val orderData = KitchenOrderDetailData(
                        orderId = orderId,
                        docPath = snapshot.reference.path,
                        tableName = tableName,
                        orderStatus = rawStatus,
                        status = if (rawStatus.equals(AppConstants.STATUS_PENDING, true)) "New" else rawStatus,
                        specialNotes = snapshot.getString(AppConstants.FIELD_SPECIAL_NOTES) ?: "",
                        orderType = orderType,
                        timestamp = snapshot.getTimestamp(AppConstants.FIELD_TIMESTAMP),
                        items = itemsList,
                        customerName = snapshot.getString(AppConstants.FIELD_CUSTOMER_NAME) ?: "",
                        customerPhone = snapshot.getString(AppConstants.FIELD_CUSTOMER_MOBILE) ?: "",
                        restaurantId = "", // Filled below
                        waiterId = snapshot.getString(AppConstants.FIELD_WAITER_ID) ?: ""
                    )

                    // Resolve restaurantId from path if missing
                    val segments = snapshot.reference.path.split("/")
                    if (segments.size > 2 && segments[0] == AppConstants.COLLECTION_USERS) {
                        orderData.restaurantId = segments[1]
                    }
                    
                    trySend(orderData)
                } catch (e: Exception) {
                    Log.e(TAG, "Manual mapping failed for workstation detail", e)
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose {
            Log.d(TAG, "listenToOrderDetails: Dismantling stream snapshot channels for path: $docPath")
            registration.remove()
        }
    }

    /**
     * Updates the order status field to "Ready" once the chef triggers completion.
     */
    suspend fun updateOrderStatusToReady(docPath: String) {
        if (docPath.isEmpty()) return
        Log.i(TAG, "updateOrderStatusToReady: Setting status field to 'Ready' for path: $docPath")
        firestore.document(docPath)
            .update(AppConstants.FIELD_ORDER_STATUS, AppConstants.STATUS_READY)
            .await()
    }

    /**
     * Updates specific items as ready in Firestore.
     */
    suspend fun updateItemsAsReady(docPath: String, updatedItems: List<Map<String, Any>>) {
        if (docPath.isEmpty()) return
        Log.i(TAG, "updateItemsAsReady: Updating items at path: $docPath")
        firestore.document(docPath)
            .update(AppConstants.FIELD_ORDER_ITEMS, updatedItems)
            .await()
    }

    suspend fun getWaiterToken(restaurantId: String, waiterId: String): String? {
        if (restaurantId.isEmpty() || waiterId.isEmpty()) return null
        return try {
            val doc = firestore.collection(AppConstants.COLLECTION_USERS)
                .document(restaurantId)
                .collection(AppConstants.COLLECTION_STAFF)
                .document(waiterId)
                .get()
                .await()
            doc.getString(AppConstants.FIELD_FCM_TOKEN)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching waiter token", e)
            null
        }
    }
}