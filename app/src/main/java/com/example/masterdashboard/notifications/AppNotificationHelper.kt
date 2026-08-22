package com.example.masterdashboard.notifications

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Standardized Helper for creating and routing all in-app notifications.
 */
object AppNotificationHelper {
    private const val TAG = "AppNotificationHelper"

    /**
     * Saves a notification record to the Firestore history.
     */
    private suspend fun saveToHistory(
        managerId: String,
        title: String,
        message: String,
        type: String,
        targetRole: String,
        targetStaffId: String = "",
        tableName: String = "",
        orderId: String = "",
        orderDocPath: String = "",
        isRead: Boolean = false
    ) {
        if (managerId.isBlank()) {
            Log.w(TAG, "saveToHistory: managerId is blank, skipping history record.")
            return
        }

        val notificationData = mapOf(
            AppConstants.FIELD_NOTIFICATION_TITLE to title,
            AppConstants.FIELD_NOTIFICATION_MESSAGE to message,
            AppConstants.FIELD_NOTIFICATION_TYPE to type,
            AppConstants.FIELD_TARGET_ROLE to targetRole,
            AppConstants.FIELD_TARGET_STAFF_ID to targetStaffId,
            AppConstants.FIELD_TABLE_NAME to tableName,
            AppConstants.FIELD_ORDER_ID to orderId,
            AppConstants.FIELD_ORDER_DOC_PATH to orderDocPath,
            AppConstants.FIELD_TIMESTAMP to Timestamp.now(),
            AppConstants.FIELD_IS_READ to isRead,
            AppConstants.FIELD_STATUS to AppConstants.STATUS_PENDING
        )

        try {
            FirebaseFirestore.getInstance().collection(AppConstants.COLLECTION_USERS)
                .document(managerId)
                .collection(AppConstants.COLLECTION_NOTIFICATIONS)
                .add(notificationData)
                .await()
            Log.d(TAG, "✅ Notification history record created for: $targetRole")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving notification history for $targetRole", e)
        }
    }

    /**
     * Notifies Manager/Owner of inventory status changes.
     */
    suspend fun notifyInventoryUpdate(
        managerId: String,
        staffName: String,
        itemName: String,
        quantity: Double,
        unit: String,
        type: String
    ) {
        if (managerId.isEmpty()) {
            Log.e(TAG, "notifyInventoryUpdate: Aborting because managerId is empty")
            return
        }

        Log.i(TAG, "Initiating Inventory update notification: item=$itemName quantity=$quantity staff=$staffName type=$type")
        val title = "Inventory $type: $itemName"
        val message = "$staffName $type $quantity $unit of $itemName in the kitchen."

        // 1. Target: Manager ONLY (As requested: Inventory updates go to manager only)
        saveToHistory(managerId, title, message, "INFORMATIONAL", "manager")

        // 2. Push Alert to Manager (Critical Only)
        if (type.contains("Stock", true)) {
            Log.d(TAG, "notifyInventoryUpdate: Stock level critical, sending push to manager")
            sendPush(managerId, title, message, "INVENTORY_UPDATE")
        }
    }

    /**
     * Notifies Kitchen of a new order entry.
     */
    suspend fun notifyKitchenOfNewOrder(
        chefTokens: List<String>,
        tableName: String,
        orderId: String,
        managerId: String,
        waiterId: String = "",
        orderDocPath: String = ""
    ) {
        Log.i(TAG, "Initiating New Order notification: orderId=$orderId table=$tableName tokensCount=${chefTokens.size}")
        val title = "New Order Received"
        val body = "New order for $tableName (#${orderId.takeLast(4)})."

        // 1. Target: Kitchen ONLY (As requested: Kitchen notifications go to chef staff only)
        saveToHistory(managerId, title, body, "ACTIONABLE_REQUEST", "kitchen", tableName = tableName, orderId = orderId, orderDocPath = orderDocPath)

        // 2. Push to Kitchen
        if (chefTokens.isNotEmpty()) {
            Log.d(TAG, "notifyKitchenOfNewOrder: Sending push to ${chefTokens.size} chef devices")
            FcmNotificationSender.sendNotification(chefTokens, title, body, mapOf("type" to "NEW_ORDER"))
        } else {
            Log.w(TAG, "notifyKitchenOfNewOrder: No chef tokens available for push")
        }
    }

    /**
     * Notifies Cashier and Waiter of a bill request.
     */
    suspend fun notifyCashierOfBillRequest(
        cashierTokens: List<String>,
        tableName: String,
        orderId: String,
        managerId: String,
        orderDocPath: String = ""
    ) {
        Log.i(TAG, "Initiating Bill Request notification: table=$tableName orderId=$orderId tokensCount=${cashierTokens.size}")
        val title = "Bill Requested: $tableName"
        val body = "Bill requested for Table $tableName."

        // 1. Target: Billing ONLY (As requested: Bill generation goes to cashier only)
        saveToHistory(managerId, title, body, "ACTIONABLE_REQUEST", "billing", tableName = tableName, orderId = orderId, orderDocPath = orderDocPath)
        
        // 2. Push to Billing
        if (cashierTokens.isNotEmpty()) {
            Log.d(TAG, "notifyCashierOfBillRequest: Sending push to ${cashierTokens.size} cashier devices")
            FcmNotificationSender.sendNotification(cashierTokens, title, body, mapOf("type" to "BILL_REQUESTED"))
        } else {
            Log.w(TAG, "notifyCashierOfBillRequest: No cashier tokens available for push")
        }
    }

    /**
     * Notifies Waiter when an order is ready for pickup.
     */
    suspend fun notifyWaiterOrderReady(
        waiterToken: String?,
        tableName: String,
        orderId: String,
        managerId: String,
        waiterId: String,
        orderDocPath: String = ""
    ) {
        Log.i(TAG, "Initiating Order Ready notification: table=$tableName orderId=$orderId waiter=$waiterId")
        val title = "Order Ready!"
        val body = "Order for Table $tableName is ready for pick-up"

        // 1. Target: Waiter (Alert)
        saveToHistory(managerId, title, body, "INFORMATIONAL", "waiter", waiterId, tableName, orderId, orderDocPath)
        
        // 2. Push to Waiter
        if (!waiterToken.isNullOrEmpty()) {
            Log.d(TAG, "notifyWaiterOrderReady: Sending push to waiter token: ${waiterToken.take(10)}...")
            FcmNotificationSender.sendNotification(listOf(waiterToken), title, body, mapOf("type" to "ORDER_READY"))
        } else {
            Log.w(TAG, "notifyWaiterOrderReady: No waiter token available for push")
        }
    }

    /**
     * Notifies relevant roles of a successful payment.
     */
    suspend fun notifyPaymentSuccess(
        managerId: String,
        tableName: String,
        orderId: String,
        amount: Double,
        waiterId: String = "",
        orderDocPath: String = ""
    ) {
        Log.i(TAG, "Initiating Payment Success notification: table=$tableName amount=$amount orderId=$orderId")
        val title = "Payment Received: $tableName"
        val body = "Bill of ₹$amount for $tableName has been paid."

        // 1. Notify Manager
        saveToHistory(managerId, title, body, "INFORMATIONAL", "manager", tableName = tableName, orderId = orderId, orderDocPath = orderDocPath)
        
        // 2. Notify Waiter (Table Release)
        saveToHistory(managerId, "Table Free: $tableName", "$tableName is now free.", "INFORMATIONAL", "waiter", targetStaffId = waiterId, tableName = tableName, orderId = orderId, orderDocPath = orderDocPath)
    }

    private suspend fun sendPush(managerId: String, title: String, body: String, type: String) {
        Log.d(TAG, "sendPush: Fetching manager token for managerId=$managerId type=$type")
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection(AppConstants.COLLECTION_USERS).document(managerId).get().await()
            val token = doc.getString(AppConstants.FIELD_FCM_TOKEN)
            if (!token.isNullOrEmpty()) {
                Log.d(TAG, "sendPush: Manager token found, sending push alert")
                FcmNotificationSender.sendNotification(listOf(token), title, body, mapOf("type" to type))
            } else {
                Log.w(TAG, "sendPush: No FCM token found for managerId=$managerId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendPush: Error fetching/sending FCM for manager", e)
        }
    }
}
