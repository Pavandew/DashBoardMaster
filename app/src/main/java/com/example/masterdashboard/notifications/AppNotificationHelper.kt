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
        tableName: String = "",
        orderId: String = "",
        orderDocPath: String = ""
    ) {
        if (managerId.isEmpty()) return

        val notificationData = mutableMapOf(
            "title" to title,
            "message" to message,
            "type" to type,
            "targetRole" to targetRole,
            "targetStaffId" to targetStaffId,
            "tableName" to tableName,
            "orderId" to orderId,
            "orderDocPath" to orderDocPath,
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
     * Notifies Manager and Owner when inventory is updated.
     */
    suspend fun notifyInventoryUpdate(
        managerId: String,
        staffName: String,
        itemName: String,
        quantity: Double,
        unit: String,
        type: String // "Added" or "Updated"
    ) {
        if (managerId.isEmpty()) {
            Log.e(TAG, "notifyInventoryUpdate: FAILED! managerId is empty.")
            return
        }

        val title = "Inventory $type: $itemName"
        val message = "$staffName $type $quantity $unit of $itemName in the kitchen."
        
        Log.d(TAG, "notifyInventoryUpdate: Starting notification flow for $managerId")

        // 1. Save to History for Manager/Owner (targetRole = "manager")
        saveToHistory(
            managerId = managerId,
            title = title,
            message = message,
            type = "INFORMATIONAL",
            targetRole = "manager"
        )

        // 2. Also notify the kitchen staff themselves so they have a log (targetRole = "kitchen")
        saveToHistory(
            managerId = managerId,
            title = "Inventory Saved",
            message = "You have $type $itemName successfully.",
            type = "INFORMATIONAL",
            targetRole = "kitchen"
        )

        // 3. Send Push Notification to Manager
        try {
            Log.d(TAG, "notifyInventoryUpdate: Fetching manager token for push alert")
            val db = FirebaseFirestore.getInstance()
            val managerDoc = db.collection(AppConstants.COLLECTION_USERS).document(managerId).get().await()
            val managerToken = managerDoc.getString(AppConstants.FIELD_FCM_TOKEN)
            
            if (!managerToken.isNullOrEmpty()) {
                Log.i(TAG, "notifyInventoryUpdate: Sending push to manager token: ${managerToken.take(10)}...")
                FcmNotificationSender.sendNotification(
                    targetTokens = listOf(managerToken),
                    title = title,
                    body = message,
                    data = mapOf("type" to "INVENTORY_UPDATE")
                )
                Log.d(TAG, "notifyInventoryUpdate: Push notification sent.")
            } else {
                Log.w(TAG, "notifyInventoryUpdate: Manager token not found, push skipped.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "notifyInventoryUpdate: Error fetching manager token", e)
        }
    }

    /**
     * Sends a notification to the kitchen staff when a new order is placed.
     */
    suspend fun notifyKitchenOfNewOrder(
        chefTokens: List<String>,
        tableName: String,
        orderId: String,
        managerId: String,
        waiterId: String = "",
        orderDocPath: String = ""
    ) {
        // Message for Kitchen Staff
        val kitchenTitle = "New Order Received"
        val kitchenBody = "New order for $tableName is ready for preparation (#${orderId.takeLast(4)})."

        // 1. Save to History for Kitchen
        saveToHistory(managerId, kitchenTitle, kitchenBody, "ACTIONABLE_REQUEST", "kitchen", tableName = tableName, orderId = orderId, orderDocPath = orderDocPath)

        // 2. Save to History for Waiter (Confirmation)
        if (waiterId.isNotEmpty()) {
            val waiterTitle = "Order Sent!"
            val waiterBody = "A new order (#${orderId.takeLast(4)}) for $tableName has been sent to the kitchen."
            saveToHistory(managerId, waiterTitle, waiterBody, "INFORMATIONAL", "waiter", targetStaffId = waiterId, tableName = tableName, orderId = orderId, orderDocPath = orderDocPath)
        }

        // 3. Send Push Notification to Kitchen (Only if chefs are online)
        if (chefTokens.isNotEmpty()) {
            Log.i(TAG, "🔔 Sending Push Alert to Kitchen staff")
            FcmNotificationSender.sendNotification(
                targetTokens = chefTokens,
                title = kitchenTitle,
                body = kitchenBody,
                data = mapOf(
                    "orderId" to orderId,
                    "tableName" to tableName,
                    "type" to "NEW_ORDER",
                    "orderDocPath" to orderDocPath
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
        waiterId: String,
        orderDocPath: String = ""
    ) {
        val title = "Order Ready!"
        val body = "Order for Table $tableName is ready for pick-up"

        // 1. Save to History (Always happens)
        saveToHistory(managerId, title, body, "INFORMATIONAL", "waiter", waiterId, tableName, orderId = orderId, orderDocPath = orderDocPath)

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
                    "type" to "ORDER_READY",
                    "orderDocPath" to orderDocPath
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
        managerId: String,
        orderDocPath: String = ""
    ) {
        val title = "Bill Requested: Table $tableName"
        val body = "A bill has been requested for Table $tableName."

        // 1. Save to History (Always happens)
        saveToHistory(managerId, title, body, "ACTIONABLE_REQUEST", "billing", tableName = tableName, orderId = orderId, orderDocPath = orderDocPath)

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
                    "type" to "BILL_REQUESTED",
                    "orderDocPath" to orderDocPath
                )
            )
        }
    }
}
