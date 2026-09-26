package com.example.masterdashboard.manager_single_res_dash.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.example.masterdashboard.utils.RestaurantPathHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManagerDashboardRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Streams all active orders across all floors, tables, and counter orders for the given manager for TODAY,
     * allowing real-time status aggregation (New, Kitchen, Ready, Served, Cancelled).
     */
    fun getActiveOrdersStream(managerId: String): Flow<List<String>> = callbackFlow {
        if (managerId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d("ManagerRepo", "Starting collectionGroup active_orders listener for managerId: $managerId")

        val registration = db.collectionGroup(AppConstants.COLLECTION_ACTIVE_ORDERS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("ManagerRepo", "Error listening to active orders collectionGroup", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdfDate.format(Date())

                val statuses = snapshots?.documents
                    ?.filter { doc ->
                        val pathMatches = doc.reference.path.contains("restaurants/$managerId")
                        val timestamp = doc.getTimestamp(AppConstants.FIELD_TIMESTAMP)?.toDate()
                            ?: doc.getTimestamp(AppConstants.FIELD_PAID_AT)?.toDate()
                        val docDate = if (timestamp != null) sdfDate.format(timestamp) else todayStr
                        
                        pathMatches && docDate == todayStr
                    }
                    ?.mapNotNull { it.getString(AppConstants.FIELD_ORDER_STATUS) } ?: emptyList()

                Log.d("ManagerRepo", "Active orders snapshot received for TODAY ($todayStr): ${statuses.size} active orders for manager $managerId")
                trySend(statuses)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Fetches restaurant name and logo URL for the given owner UID/restaurant ID.
     * Returns Pair(Name, LogoUrl)
     */
    suspend fun getRestaurantDetails(ownerUid: String): Pair<String?, String?> {
        return try {
            val doc = RestaurantPathHelper.getRestaurantDocRef(ownerUid).get().await()
            val name = doc.getString(AppConstants.FIELD_RESTAURANT_NAME)
            val logo = doc.getString("restaurantLogoUri")
                ?: doc.getString("restaurantLogoUrl")
                ?: (doc.get("billingPrinterSettings") as? Map<*, *>)?.get("restaurantLogoUri") as? String
            name to logo
        } catch (e: Exception) {
            Log.e("ManagerRepo", "Error fetching restaurant details for $ownerUid", e)
            null to null
        }
    }
}
