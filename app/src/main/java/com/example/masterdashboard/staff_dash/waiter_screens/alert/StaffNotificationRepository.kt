package com.example.masterdashboard.staff_dash.waiter_screens.alert

import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StaffNotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Streams real-time notifications for a specific manager.
     */
    fun getNotificationsStream(managerId: String): Flow<List<StaffAlertItem>> = callbackFlow {
        val listener = db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val alerts = snapshots?.mapNotNull { doc ->
                    val ts = doc.getTimestamp("timestamp")
                    val timeStr = if (ts != null) {
                        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        sdf.format(ts.toDate())
                    } else "Just now"

                    StaffAlertItem(
                        id = doc.id,
                        tableId = doc.getString("tableName") ?: "",
                        title = doc.getString("title") ?: "Alert",
                        message = doc.getString("message") ?: "",
                        timeStamp = timeStr,
                        type = try { NotificationType.valueOf(doc.getString("type") ?: "INFORMATIONAL") } catch (e: Exception) { NotificationType.INFORMATIONAL },
                        status = try { RequestStatus.valueOf(doc.getString("status") ?: "PENDING") } catch (e: Exception) { RequestStatus.PENDING },
                        isRead = doc.getBoolean("isRead") ?: false,
                        targetRole = doc.getString("targetRole") ?: "",
                        targetStaffId = doc.getString("targetStaffId") ?: ""
                    )
                } ?: emptyList()

                trySend(alerts)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Updates the status and read state of a specific notification.
     */
    suspend fun updateNotificationStatus(managerId: String, alertId: String, status: RequestStatus) {
        db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection("notifications")
            .document(alertId)
            .update("status", status.name, "isRead", true)
            .await()
    }
}
