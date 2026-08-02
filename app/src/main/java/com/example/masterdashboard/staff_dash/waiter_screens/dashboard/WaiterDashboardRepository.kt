package com.example.masterdashboard.staff_dash.waiter_screens.dashboard

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WaiterDashboardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "StaffDashboardRepo"
    }

    /**
     * Streams real-time counts for active tables, pending orders, ready orders, and total served.
     */
    fun streamDashboardMetrics(managerId: String): Flow<DashboardMetrics> = callbackFlow {
        Log.d(TAG, "streamDashboardMetrics: Registering Firestore snapshot listener for manager: $managerId")

        // Listener for active orders group across tables
        val ordersRegistration = firestore.collectionGroup("active_orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to active_orders stream", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var pendingCount = 0
                    var readyCount = 0
                    var servedCount = 0

                    for (doc in snapshot.documents) {
                        when (doc.getString("orderStatus")?.uppercase()) {
                            "PENDING", "PREPARING", "NEW" -> pendingCount++
                            "READY" -> readyCount++
                            "SERVED", "COMPLETED" -> servedCount++
                        }
                    }

                    // Query occupied tables count
                    firestore.collectionGroup("floor_tables")
                        .whereEqualTo("status", "OCCUPIED")
                        .get()
                        .addOnSuccessListener { tableSnapshot ->
                            val activeTablesCount = tableSnapshot.size()

                            val metrics = DashboardMetrics(
                                activeTables = String.format("%02d", activeTablesCount),
                                pendingOrders = String.format("%02d", pendingCount),
                                readyOrders = String.format("%02d", readyCount),
                                totalServed = String.format("%02d", servedCount)
                            )
                            trySend(metrics)
                        }
                        .addOnFailureListener {
                            val metrics = DashboardMetrics(
                                activeTables = "00",
                                pendingOrders = String.format("%02d", pendingCount),
                                readyOrders = String.format("%02d", readyCount),
                                totalServed = String.format("%02d", servedCount)
                            )
                            trySend(metrics)
                        }
                }
            }

        awaitClose {
            Log.d(TAG, "Dismantling dashboard metrics snapshot listener.")
            ordersRegistration.remove()
        }
    }

    /**
     * Streams top 5 recent activity log alerts from Firestore.
     */
    fun streamRecentActivities(managerId: String, limit: Long = 5): Flow<List<RecentActivityItem>> = callbackFlow {
        val activityRef = firestore.collection("users")
            .document(managerId)
            .collection("activity_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

        val registration = activityRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to activity_logs stream", error)
                close(error)
                return@addSnapshotListener
            }

            val logs = snapshot?.documents?.mapNotNull { doc ->
                RecentActivityItem(
                    id = doc.id,
                    title = doc.getString("tableName") ?: "Table",
                    subtitle = doc.getString("message") ?: "Activity logged",
                    timestampText = doc.getString("timeAgo") ?: "Just now",
                    type = doc.getString("type") ?: "ORDER"
                )
            } ?: emptyList()

            trySend(logs)
        }

        awaitClose { registration.remove() }
    }
}

data class DashboardMetrics(
    val activeTables: String = "00",
    val pendingOrders: String = "00",
    val readyOrders: String = "00",
    val totalServed: String = "00"
)