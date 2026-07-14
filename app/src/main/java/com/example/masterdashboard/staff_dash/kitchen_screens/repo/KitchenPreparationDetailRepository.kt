package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.Timestamp
import java.util.Date

class KitchenPreparationDetailRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "KitchenWorkstationRepo"
    }

    /**
     * Streams real-time ticket modifications for a specific document.
     */
    fun listenToOrderDetails(orderId: String): Flow<KitchenOrderDetailData?> = callbackFlow {
        Log.d(TAG, "listenToOrderDetails: Registering Firestore snapshot channel on document path: orders/$orderId")

        val docRef = firestore.collection("orders").document(orderId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore snapshot collection dropped for ID: $orderId", error)
                close(error)
                return@addSnapshotListener
            }

            val orderData = snapshot?.toObject(KitchenOrderDetailData::class.java)?.copy(orderId = snapshot.id)
            trySend(orderData)
        }

        awaitClose {
            Log.d(TAG, "listenToOrderDetails: Dismantling stream snapshot channels for ID: $orderId")
            registration.remove()
        }
    }

    /**
     * Mutates the order status field to "Ready" once the chef triggers completion.
     */
    suspend fun updateOrderStatusToReady(orderId: String) {
        Log.i(TAG, "updateOrderStatusToReady: Commencing server mutation. Setting status field to 'Ready' for orderId: $orderId")
        firestore.collection("orders")
            .document(orderId)
            .update("status", "Ready")
            .await()
    }
}