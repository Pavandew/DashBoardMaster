package com.restroone.app.staff_dash.billing_screens.repo

import android.util.Log
import com.restroone.app.utils.AppConstants
import com.restroone.app.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.restroone.app.staff_dash.waiter_screens.table.models.OrderItemModel
import com.restroone.app.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.restroone.app.utils.RestaurantPathHelper
import com.restroone.app.utils.RestaurantActiveOrders
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class CashierBillingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val functions = FirebaseFunctions.getInstance()
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
            val twentyFourHoursAgoMillis = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            val combined = (activeOrdersList + completedOrdersList)
                .filter { order ->
                    val status = order.orderStatus.uppercase()
                    val isPaidOrCompleted = status == "PAID" || status == "COMPLETED"
                    val isWithin24Hours = (order.timestamp.toDate().time) >= twentyFourHoursAgoMillis
                    !isPaidOrCompleted || isWithin24Hours
                }
                .distinctBy { if (it.orderId.isNotEmpty() && it.orderId != "N/A") it.orderId else it.docPath }
            trySend(combined)
        }

        val targetPrefix = "restaurants/$managerId/"

        // 1. Listen to active_orders across table subcollections for this manager
        val activeJob = launch {
            RestaurantActiveOrders.observe(firestore, managerId).collect { allDocs ->
                val documents = allDocs.filter { it.reference.path.startsWith(targetPrefix) }
                launch(Dispatchers.IO) {
                    activeOrdersList = documents.map { doc ->
                        async { parseOrderDocument(doc) }
                    }.awaitAll()

                    emitCombinedList()
                }
            }
        }

        // 2. Listen to completed_orders collection for this manager (last 24 hours)
        val twentyFourHoursAgo = Timestamp(
            java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        )

        val completedRegistration = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_COMPLETED_ORDERS)
            .whereGreaterThanOrEqualTo(AppConstants.FIELD_TIMESTAMP, twentyFourHoursAgo)
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
            activeJob.cancel()
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
                !tableName.isNullOrEmpty() -> tableName
                else -> "Counter Order"
            }

            val orderStatus = doc.getString(AppConstants.FIELD_ORDER_STATUS) ?: AppConstants.STATUS_SERVED
            val subtotal = (doc.get(AppConstants.FIELD_SUBTOTAL) as? Number)?.toDouble() ?: 0.0
            val serviceChargeAmount = (doc.get(AppConstants.FIELD_SERVICE_CHARGE) as? Number)?.toDouble() ?: 0.0
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

            val summaryStr = itemsList.joinToString(", ") { item ->
                val name = when {
                    item.variantName.isEmpty() -> item.itemName
                    item.itemName.contains("(${item.variantName})", ignoreCase = true) || item.itemName.contains(item.variantName, ignoreCase = true) -> item.itemName
                    else -> "${item.itemName} (${item.variantName})"
                }
                "${item.quantity}x $name"
            }

            CashierBillingOrderModel(
                orderId = orderId,
                tableId = tableId,
                tableName = finalDisplayName,
                orderType = orderType,
                orderStatus = orderStatus,
                itemsSummary = summaryStr,
                items = itemsList,
                subtotal = subtotal,
                serviceChargeAmount = serviceChargeAmount,
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
        Log.d(TAG, "fetchOrderDetails: Fetching details from path: '$docPath'")
        emit(ResourceUiState.Loading)
        try {
            var doc = firestore.document(docPath).get().await()
            if (!doc.exists()) {
                // If deleted from active_orders, check completed_orders
                val pathSegments = docPath.split("/")
                val managerId = if (pathSegments.size >= 2) pathSegments[1] else ""
                val orderId = docPath.substringAfterLast("/")
                if (managerId.isNotEmpty()) {
                    val completedRef = RestaurantPathHelper.getOutletDocRef(managerId)
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
        try {
            val restaurantId = order.docPath.split("/").getOrNull(1)
                ?.takeIf { order.docPath.startsWith("restaurants/$it/") }
                ?: throw IllegalArgumentException("Invalid active-order path")
            val result = functions.getHttpsCallable("settleRestaurantOrder").call(
                mapOf(
                    "restaurantId" to restaurantId,
                    "docPath" to order.docPath,
                    "paymentMethod" to paymentMode,
                    "discountAmount" to discount,
                    "applyServiceCharge" to (order.serviceChargeAmount > 0.0)
                )
            ).await()
            val response = result.data as? Map<*, *>
            val completedOrderId = response?.get("completedOrderId") as? String
                ?: throw IllegalStateException("Settlement service returned no completed-order ID")
            trySend(ResourceUiState.Success(
                "restaurants/$restaurantId/${AppConstants.COLLECTION_COMPLETED_ORDERS}/$completedOrderId"
            ))
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Settlement callable failed for order ${order.orderId}", e)
            trySend(ResourceUiState.Error(e.message ?: "Settlement failed"))
            close()
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

        val restaurantId = order.docPath.split("/").getOrNull(1)
            ?.takeIf { order.docPath.startsWith("restaurants/$it/completed_orders/") }
        if (restaurantId.isNullOrBlank()) {
            trySend(ResourceUiState.Error("Invalid completed-order path"))
            close()
            return@callbackFlow
        }

        try {
            functions.getHttpsCallable("completeTakeawayOrder").call(
                mapOf("restaurantId" to restaurantId, "docPath" to order.docPath)
            ).await()
            trySend(ResourceUiState.Success("Order Handed Over"))
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete takeaway order ${order.orderId}", e)
            trySend(ResourceUiState.Error(e.message ?: "Update failed"))
            close(e)
        }
        awaitClose { }
    }
}
