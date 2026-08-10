package com.example.masterdashboard.notifications

import android.util.Log
import com.example.masterdashboard.login.utils.AppConstants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

object AppNotificationHelper {
    private const val TAG = "AppNotificationHelper"

    /**
     * Saves notification to Firestore central history
     */
    private fun saveToHistory(
        managerId: String,
        title: String,
        message: String,
        type: String,
        targetRole: String,
        targetStaffId: String = "",
        tableName: String = ""
    ) {
        val notificationData = mapOf(
            "title" to title,
            "message" to message,
            "type" to type, 
            "targetRole" to targetRole,
            "targetStaffId" to targetStaffId,
            "tableName" to tableName,
            "timestamp" to Timestamp.now(),
            "isRead" to false,
            "status" to "PENDING"
        )

        FirebaseFirestore.getInstance().collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener { Log.d(TAG, "Notification saved to history") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save notification history", e) }
    }

    /**
     * Sends a notification to the kitchen staff when a new order is placed.
     */
    suspend fun notifyKitchenOfNewOrder(
        chefTokens: List<String>,
        tableName: String,
        orderId: String,
        managerId: String 
    ) {
        val title = "New Order: Table $tableName"
        val body = "A new order (#${orderId.takeLast(4)}) has been sent to the kitchen."

        // 1. Save to History (Always do this)
        saveToHistory(managerId, title, body, "ACTIONABLE_REQUEST", "kitchen", tableName = tableName)

        // 2. Send Push Notification (Only if tokens exist)
        if (chefTokens.isEmpty()) {
            Log.w(TAG, "notifyKitchenOfNewOrder: No chef tokens found. Push notification skipped, but history saved.")
            return
        }

        Log.i(TAG, "🔔 Notifying Kitchen: Table $tableName, Order $orderId")
        
        FcmNotificationSender.sendNotification(
            targetTokens = chefTokens,
            title = title,
            body = body,
            data = mapOf(
                "orderId" to orderId,
                "tableName" to tableName,
                "type" to "NEW_ORDER"
            )
        )
    }

    /**
     * Sends a notification to the waiter when their order is ready.
     */
    suspend fun notifyWaiterOrderReady(
        waiterToken: String,
        tableName: String,
        orderId: String,
        managerId: String, 
        waiterId: String   
    ) {
        Log.i(TAG, "🔔 Notifying Waiter: Table $tableName, Order $orderId")
        val title = "Order Ready!"
        val body = "Order for Table $tableName is ready for pick-up"

        FcmNotificationSender.sendNotification(
            targetTokens = listOf(waiterToken),
            title = title,
            body = body,
            data = mapOf(
                "orderId" to orderId,
                "tableName" to tableName,
                "type" to "ORDER_READY"
            )
        )

        saveToHistory(managerId, title, body, "ORDER_READY", "waiter", waiterId, tableName)
    }

    /**
     * Sends a notification to the cashier when a bill is requested for a table.
     */
    suspend fun notifyCashierOfBillRequest(
        cashierTokens: List<String>,
        tableName: String,
        orderId: String,
        managerId: String 
    ) {
        val title = "Bill Requested: Table $tableName"
        val body = "A bill has been requested for Table $tableName."

        // 1. Save to History
        saveToHistory(managerId, title, body, "ACTIONABLE_REQUEST", "billing", tableName = tableName)

        // 2. Send Push Notification
        if (cashierTokens.isEmpty()) {
            Log.w(TAG, "notifyCashierOfBillRequest: No cashier tokens found. Push notification skipped.")
            return
        }

        Log.i(TAG, "🔔 Notifying Cashier: Table $tableName, Order $orderId")
        
        FcmNotificationSender.sendNotification(
            targetTokens = cashierTokens,
            title = title,
            body = body,
            data = mapOf(
                "orderId" to orderId,
                "tableName" to tableName,
                "type" to "BILL_REQUESTED"
            )
        )
    }
}
