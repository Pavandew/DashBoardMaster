package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class KitchenPreparationDetailRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "KitchenWorkstationRepo"
    }

    /**
     * Streams real-time ticket modifications for a specific document using its full path.
     */
    fun listenToOrderDetails(docPath: String): Flow<KitchenOrderDetailData?> = callbackFlow {
        if (docPath.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        Log.d(TAG, "listenToOrderDetails: Registering Firestore snapshot channel on path: $docPath")

        val docRef = firestore.document(docPath)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore snapshot collection dropped for path: $docPath", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val orderData = snapshot.toObject(KitchenOrderDetailData::class.java)
                if (orderData != null) {
                    orderData.orderId = snapshot.id
                    orderData.docPath = snapshot.reference.path
                    
                    val rawStatus = snapshot.getString("orderStatus") ?: "New"
                    orderData.status = if (rawStatus.equals("PENDING", true)) "New" else rawStatus
                    
                    trySend(orderData)
                } else {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose {
            Log.d(TAG, "listenToOrderDetails: Dismantling stream snapshot channels for path: $docPath")
            registration.remove()
        }
    }

    /**
     * Updates the order status field to "Ready" once the chef triggers completion.
     */
    suspend fun updateOrderStatusToReady(docPath: String) {
        if (docPath.isEmpty()) return
        Log.i(TAG, "updateOrderStatusToReady: Setting status field to 'Ready' for path: $docPath")
        firestore.document(docPath)
            .update("orderStatus", "Ready")
            .await()
    }

    /**
     * Updates specific items as ready in Firestore.
     */
    suspend fun updateItemsAsReady(docPath: String, updatedItems: List<Map<String, Any>>) {
        if (docPath.isEmpty()) return
        Log.i(TAG, "updateItemsAsReady: Updating items at path: $docPath")
        firestore.document(docPath)
            .update("items", updatedItems)
            .await()
    }
}