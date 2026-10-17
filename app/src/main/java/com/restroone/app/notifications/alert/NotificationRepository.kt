package com.restroone.app.notifications.alert

import android.util.Log
import com.restroone.app.staff_dash.utils.TimeUtils
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.RestaurantPathHelper
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
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
    private val functions = FirebaseFunctions.getInstance()

    /**
     * Streams real-time notifications for a specific manager/outlet, filtered by role for efficiency.
     */
    fun getNotificationsStream(managerId: String, role: String, staffId: String): Flow<List<AppNotificationModel>> = callbackFlow {
        val userRole = role.lowercase().trim()
        val isManager = userRole == "manager" || userRole == "owner_single" || userRole == "owner_multi"
        Log.d(TAG, "Starting notifications stream for managerId=$managerId role=$userRole staffId=$staffId isManager=$isManager")

        val roleTargets = when (userRole) {
            "waiter", "head waiter", "waiter_staff" ->
                listOf("all", "waiter", "head waiter", "waiter_staff")
            "chef", "master chef", "kitchen", "chef_staff" ->
                listOf("all", "chef", "master chef", "kitchen", "chef_staff")
            "cashier", "billing" -> listOf("all", "cashier", "billing")
            else -> listOf("all", userRole)
        }

        val query = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_NOTIFICATIONS)
            .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)

        val targets = if (isManager) {
            listOf("all", "manager")
        } else {
            roleTargets.distinct()
        }

        Log.d(TAG, "Applying server-side filter for targets: $targets")
        val finalQuery = query.whereIn(AppConstants.FIELD_TARGET_ROLE, targets).limit(50)
        var broadcastDocs: List<DocumentSnapshot> = emptyList()
        var targetedDocs: List<DocumentSnapshot> = emptyList()
        fun emitAlerts() {
            val alerts = (broadcastDocs + targetedDocs).distinctBy { it.id }.mapNotNull { doc ->
                val target = doc.getString(AppConstants.FIELD_TARGET_ROLE)?.lowercase()?.trim() ?: ""
                val tStaffId = doc.getString(AppConstants.FIELD_TARGET_STAFF_ID) ?: ""
                val ts = doc.getTimestamp(AppConstants.FIELD_TIMESTAMP)
                val timeStr = TimeUtils.getRelativeTime(ts)

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
            }

            Log.d(TAG, "Notification snapshot received. Valid alerts after filtering: ${alerts.size} for managerId=$managerId")
            trySend(alerts)
        }

        val listeners = if (isManager) {
            listOf(finalQuery.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Notification stream error for managerId=$managerId", error)
                    close(error)
                    return@addSnapshotListener
                }
                broadcastDocs = snapshots?.documents.orEmpty()
                emitAlerts()
            })
        } else {
            listOf(
                finalQuery.whereEqualTo(AppConstants.FIELD_TARGET_STAFF_ID, "")
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.e(TAG, "Broadcast notification stream error for managerId=$managerId", error)
                            close(error)
                            return@addSnapshotListener
                        }
                        broadcastDocs = snapshots?.documents.orEmpty()
                        emitAlerts()
                    },
                query.whereIn(AppConstants.FIELD_TARGET_ROLE, targets.filter { it != "all" })
                    .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                    .limit(50)
                    .whereEqualTo(AppConstants.FIELD_TARGET_STAFF_ID, staffId)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.e(TAG, "Targeted notification stream error for managerId=$managerId", error)
                            close(error)
                            return@addSnapshotListener
                        }
                        targetedDocs = snapshots?.documents.orEmpty()
                        emitAlerts()
                    }
            )
        }

        awaitClose {
            Log.d(TAG, "Stopping notifications stream for managerId=$managerId")
            listeners.forEach { it.remove() }
        }
    }

    /**
     * Updates the status and read state of a specific notification.
     */
    suspend fun updateNotificationStatus(managerId: String, alertId: String, status: RequestStatus) {
       Log.d(TAG, "Updating notification status: managerId=$managerId alertId=$alertId status=${status.name}")
       try {
           functions.getHttpsCallable("updateStaffNotificationStatus").call(
               mapOf(
                   "restaurantId" to managerId,
                   "notificationId" to alertId,
                   "status" to status.name
               )
           ).await()
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
           RestaurantPathHelper.getOutletDocRef(managerId)
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
