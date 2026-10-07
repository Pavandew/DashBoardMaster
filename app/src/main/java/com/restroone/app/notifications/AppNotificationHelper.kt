package com.restroone.app.notifications

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Standardized Helper for creating and routing all in-app notifications.
 */
object AppNotificationHelper {
    private const val TAG = "AppNotificationHelper"
    private val functions = FirebaseFunctions.getInstance()

    /**
     * Saves a notification record to the Firestore history inside restaurants/{restaurantId}/notifications.
     */
    private suspend fun saveToHistory(
        managerId: String,
        event: String,
        orderDocPath: String = "",
        inventoryId: String = ""
    ) {
        if (managerId.isBlank()) {
            Log.w(TAG, "saveToHistory: managerId is blank, skipping history record.")
            return
        }

        try {
            functions.getHttpsCallable("createStaffNotification").call(
                mapOf(
                    "restaurantId" to managerId,
                    "event" to event,
                    "orderDocPath" to orderDocPath,
                    "inventoryId" to inventoryId
                )
            )
                .await()
            Log.d(TAG, "Notification history event created: $event at restaurants/$managerId/notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification history event $event", e)
            throw e
        }
    }

    /**
     * Notifies Manager/Owner of inventory status changes.
     */
    suspend fun notifyInventoryUpdate(
        managerId: String,
        inventoryId: String
    ) {
        if (managerId.isEmpty()) {
            Log.e(TAG, "notifyInventoryUpdate: Aborting because managerId is empty")
            return
        }

        saveToHistory(managerId, "INVENTORY_UPDATE", inventoryId = inventoryId)
    }

    /**
     * Notifies Kitchen of a new order entry.
     */
    suspend fun notifyKitchenOfNewOrder(
        managerId: String,
        orderDocPath: String = ""
    ) {
        saveToHistory(managerId, "KITCHEN_NEW_ORDER", orderDocPath = orderDocPath)
    }

    /**
     * Notifies Cashier and Waiter of a bill request.
     */
    suspend fun notifyCashierOfBillRequest(
        managerId: String,
        orderDocPath: String = ""
    ) {
        saveToHistory(managerId, "BILL_REQUEST", orderDocPath = orderDocPath)
    }

    /**
     * Notifies Waiter when an order is ready for pickup.
     */
    suspend fun notifyWaiterOrderReady(
        managerId: String,
        orderDocPath: String = ""
    ) {
        saveToHistory(managerId, "ORDER_READY", orderDocPath = orderDocPath)
    }

    /**
     * Notifies relevant roles of a successful payment.
     */
    suspend fun notifyPaymentSuccess(
        managerId: String,
        orderDocPath: String = ""
    ) {
        saveToHistory(managerId, "PAYMENT_SUCCESS", orderDocPath = orderDocPath)
    }
}
