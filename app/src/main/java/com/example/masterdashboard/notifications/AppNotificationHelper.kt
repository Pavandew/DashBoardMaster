package com.example.masterdashboard.notifications

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AppNotificationHelper {
    private const val TAG = "AppNotificationHelper"

    /**
     * Saves notification to Firestore central history.
     * This ensures staff can see alerts in their history list even if they were logged out.
     */
    private suspend fun saveToHistory(
        managerId: String,
        title: String,
        message: String,
        type: String,
        targetRole: String,
        targetStaffId: String = "",
        tableName: String = ""
    ) {
        if (managerId.isEmpty()) return

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

        try {
            FirebaseFirestore.getInstance().collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection("notifications")
                .add(notificationData)
                .await()
            Log.d(TAG, "✅ Notification history record created for: $targetRole")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving notification history", e)
        }
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

        // 1. Save to History (Always happens)
        saveToHistory(managerId, title, body, "ACTIONABLE_REQUEST", "kitchen", tableName = tableName)

        // 2. Send Push Notification (Only if chefs are online)
        if (chefTokens.isNotEmpty()) {
            Log.i(TAG, "🔔 Sending Push Alert to Kitchen staff")
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
    }

    /**
     * Sends a notification to the waiter when their order is ready.
     */
    suspend fun notifyWaiterOrderReady(
        waiterToken: String?,
        tableName: String,
        orderId: String,
        managerId: String, 
        waiterId: String   
    ) {
        val title = "Order Ready!"
        val body = "Order for Table $tableName is ready for pick-up"

        // 1. Save to History (Always happens)
        saveToHistory(managerId, title, body, "INFORMATIONAL", "waiter", waiterId, tableName)

        // 2. Send Push Notification (Only if waiter is online)
        if (!waiterToken.isNullOrEmpty()) {
            Log.i(TAG, "🔔 Sending Push Alert to Waiter: $waiterId")
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
        }
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

        // 1. Save to History (Always happens)
        saveToHistory(managerId, title, body, "ACTIONABLE_REQUEST", "billing", tableName = tableName)

        // 2. Send Push Notification (Only if cashiers are online)
        if (cashierTokens.isNotEmpty()) {
            Log.i(TAG, "🔔 Sending Push Alert to Billing staff")
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
}
