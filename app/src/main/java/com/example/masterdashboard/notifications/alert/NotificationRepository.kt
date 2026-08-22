package com.example.masterdashboard.notifications.alert

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Optimized repository for streaming and managing role-based notifications.
 */
class NotificationRepository {
    companion object {
        private const val TAG = "NotificationRepository"
    }

    private val db = FirebaseFirestore.getInstance()

    /**
     * Streams real-time notifications for a specific manager, filtered by role for efficiency.
     */
    fun getNotificationsStream(managerId: String, role: String, staffId: String): Flow<List<AppNotificationModel>> = callbackFlow {
        val userRole = role.lowercase().trim()
        Log.d(TAG, "Starting notifications stream for managerId=$managerId role=$userRole staffId=$staffId")

        // Build role-based filter list
        val roleTargets = mutableListOf("all", userRole)
        when (userRole) {
            "waiter", "waiter_staff" -> roleTargets.addAll(listOf("waiter", "waiter_staff"))
            "chef", "kitchen" -> roleTargets.addAll(listOf("chef", "kitchen"))
            "cashier", "billing" -> roleTargets.addAll(listOf("cashier", "billing"))
        }

        val query = db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_NOTIFICATIONS)
            .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(50) // Optimization: Only show recent 50 alerts

        val listener = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Notification stream error for managerId=$managerId", error)
                close(error)
                return@addSnapshotListener
            }

            val alerts = snapshots?.mapNotNull { doc ->
                val target = doc.getString(AppConstants.FIELD_TARGET_ROLE)?.lowercase()?.trim() ?: ""
                val tStaffId = doc.getString(AppConstants.FIELD_TARGET_STAFF_ID) ?: ""

                // Server-side filtering logic
                val isManager = userRole == "manager" || userRole == "owner_single" || userRole == "owner_multi"

                val shouldShow = when {
                    isManager -> true // Managers see everything
                    tStaffId.isNotEmpty() -> tStaffId == staffId
                    target in roleTargets -> true
                    else -> false
                }

                if (!shouldShow) return@mapNotNull null

                val ts = doc.getTimestamp(AppConstants.FIELD_TIMESTAMP)
                val timeStr = if (ts != null) {
                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    sdf.format(ts.toDate())
                } else "Just now"

                AppNotificationModel(
                    id = doc.id,
                    tableId = doc.getString(AppConstants.FIELD_TABLE_NAME) ?: "",
                    title = doc.getString(AppConstants.FIELD_NOTIFICATION_TITLE) ?: "Alert",
                    message = doc.getString(AppConstants.FIELD_NOTIFICATION_MESSAGE) ?: "",
                    timeStamp = timeStr,
                    type = try { NotificationType.valueOf(doc.getString(AppConstants.FIELD_NOTIFICATION_TYPE) ?: "INFORMATIONAL") } catch (e: Exception) { NotificationType.INFORMATIONAL },
                    status = try { RequestStatus.valueOf(doc.getString(AppConstants.FIELD_STATUS) ?: "PENDING") } catch (e: Exception) { RequestStatus.PENDING },
                    isRead = doc.getBoolean(AppConstants.FIELD_IS_READ) ?: false,
                    targetRole = target,
                    targetStaffId = tStaffId,
                    orderId = doc.getString(AppConstants.FIELD_ORDER_ID) ?: "",
                    orderDocPath = doc.getString(AppConstants.FIELD_ORDER_DOC_PATH) ?: ""
                )
            } ?: emptyList()

            Log.d(TAG, "Notification snapshot received. Valid alerts after filtering: ${alerts.size} for managerId=$managerId")
            trySend(alerts)
        }

        awaitClose {
            Log.d(TAG, "Stopping notifications stream for managerId=$managerId")
            listener.remove()
        }
    }

    /**
     * Updates the status and read state of a specific notification.
     */
    suspend fun updateNotificationStatus(managerId: String, alertId: String, status: RequestStatus) {
       Log.d(TAG, "Updating notification status: managerId=$managerId alertId=$alertId status=${status.name}")
       try {
           db.collection(AppConstants.COLLECTION_USERS)
               .document(managerId)
               .collection(AppConstants.COLLECTION_NOTIFICATIONS)
               .document(alertId)
               .update(AppConstants.FIELD_STATUS, status.name, AppConstants.FIELD_IS_READ, true)
               .await()
           Log.d(TAG, "Notification status updated successfully: alertId=$alertId")
       } catch (e: Exception) {
           Log.e(TAG, "Failed to update notification status: alertId=$alertId", e)
           throw e
       }
    }

    /**
     * Marks a notification as read without changing its status.
     */
    suspend fun markAsRead(managerId: String, alertId: String) {
       Log.d(TAG, "Marking notification as read: managerId=$managerId alertId=$alertId")
       try {
           db.collection(AppConstants.COLLECTION_USERS)
               .document(managerId)
               .collection(AppConstants.COLLECTION_NOTIFICATIONS)
               .document(alertId)
               .update(AppConstants.FIELD_IS_READ, true)
               .await()
           Log.d(TAG, "Notification marked as read: alertId=$alertId")
       } catch (e: Exception) {
           Log.e(TAG, "Failed to mark notification as read: alertId=$alertId", e)
           throw e
       }
    }
}
