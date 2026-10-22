package com.restroone.app.notifications.alert

import android.util.Log
import com.restroone.app.staff_dash.utils.TimeUtils
import com.restroone.app.utils.AppConstants
import com.restroone.app.utils.RestaurantPathHelper
import com.google.firebase.functions.FirebaseFunctions
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
    private val functions = FirebaseFunctions.getInstance()

    /**
     * Streams real-time notifications for a specific manager/outlet.
     * Note: Access control, role authorization, and notification creation are handled
     * securely by backend Cloud Functions (createStaffNotification, updateStaffNotificationStatus)
     * and Firestore Security Rules (canReadNotification in firestore.rules).
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

        val allowedTargets = if (isManager) {
            listOf("all", "manager", "kitchen", "chef", "master chef", "chef_staff", "waiter", "head waiter", "waiter_staff", "cashier", "billing")
        } else {
            roleTargets.distinct()
        }

        val notificationsCollection = RestaurantPathHelper.getOutletDocRef(managerId)
            .collection(AppConstants.COLLECTION_NOTIFICATIONS)

        val listener = notificationsCollection
            .orderBy(AppConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Notification stream error for managerId=$managerId", error)
                    close(error)
                    return@addSnapshotListener
                }

                val docs = snapshots?.documents.orEmpty()
                Log.d(TAG, "Raw snapshot received: ${docs.size} docs from Firestore path 'restaurants/$managerId/notifications'")

                val alerts = docs.mapNotNull { doc ->
                    val target = doc.getString(AppConstants.FIELD_TARGET_ROLE)?.lowercase()?.trim() ?: ""
                    val tStaffId = doc.getString(AppConstants.FIELD_TARGET_STAFF_ID) ?: ""

                    Log.d(TAG, "Inspecting doc id=${doc.id}, title='${doc.getString("title")}', targetRole='$target', targetStaffId='$tStaffId'")

                    // 1. Role Filter: Match allowed target roles for current user role
                    if (!allowedTargets.contains(target)) {
                        Log.d(TAG, "Filtered out doc ${doc.id}: targetRole '$target' not in allowedTargets $allowedTargets")
                        return@mapNotNull null
                    }

                    // 2. Staff ID Filter for non-managers:
                    // If notification is targeted to a specific staff member, match with current user's staffId
                    if (!isManager && tStaffId.isNotEmpty() && tStaffId != staffId) {
                        Log.d(TAG, "Filtered out doc ${doc.id}: targetStaffId '$tStaffId' != current staffId '$staffId'")
                        return@mapNotNull null
                    }

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

                Log.d(TAG, "Notification snapshot received. Filtered alerts: ${alerts.size} for managerId=$managerId role=$userRole")
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
