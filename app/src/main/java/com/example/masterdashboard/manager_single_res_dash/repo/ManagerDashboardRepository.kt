package com.example.masterdashboard.manager_single_res_dash.repo

import android.util.Log
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ManagerDashboardRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Streams all active orders for the given manager to allow real-time status aggregation.
     */
    fun getActiveOrdersStream(managerId: String): Flow<List<String>> = callbackFlow {
        val registration = db.collection(AppConstants.COLLECTION_USERS)
            .document(managerId)
            .collection(AppConstants.COLLECTION_ACTIVE_ORDERS)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val statuses = snapshots?.mapNotNull { it.getString(AppConstants.FIELD_ORDER_STATUS) } ?: emptyList()
                trySend(statuses)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Fetches the restaurant name for the given owner UID.
     */
    suspend fun getRestaurantName(ownerUid: String): String? {
        return try {
            val doc = db.collection(AppConstants.COLLECTION_USERS).document(ownerUid).get().await()
            doc.getString(AppConstants.FIELD_RESTAURANT_NAME)
        } catch (e: Exception) {
            Log.e("ManagerRepo", "Error fetching restaurant name", e)
            null
        }
    }
}
