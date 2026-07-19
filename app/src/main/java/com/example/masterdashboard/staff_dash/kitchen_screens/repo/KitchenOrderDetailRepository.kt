package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import android.util.Log
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class KitchenOrderDetailsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "KitchenDetailsRepo"
    }

    /**
     * Emits premium local mock data instantly based on the passed orderId
     * to check layout thresholds before connecting live remote paths.
     */
    fun getLiveOrderDetails(orderId: String): Flow<KitchenOrderDetailData?> = callbackFlow {
        Log.i(TAG, "getLiveOrderDetails: Intercepting query line. Generating clean light-theme static data for orderId: $orderId")

        // 1. Generate local dummy detail entries to feed your high-contrast text views
        val staticMockOrder = when {
            orderId.contains("9841", ignoreCase = true) || orderId.endsWith("9841") -> {
                KitchenOrderDetailData(
                    orderId = orderId,
                    tableName = "Table - 4",
                    status = "New",
                    orderNote = "Deliver everything together. Make sure the burgers have extra cheese slices.",
                    timestamp = Timestamp(Date()),
                    items = listOf(
                        OrderDetailItem("Crispy Chicken Burger", 2, "No onions", 250.0, "Non-Veg"),
                        OrderDetailItem("Loaded Cheesy Fries", 1, "Extra spicy", 150.0, "Veg"),
                        OrderDetailItem("Classic Cold Coffee", 1, "", 120.0, "Veg")
                    )
                )
            }
            orderId.contains("8722", ignoreCase = true) || orderId.endsWith("8722") -> {
                KitchenOrderDetailData(
                    orderId = orderId,
                    tableName = "Table - 12",
                    status = "Preparing",
                    orderNote = "Appetizers first please. Drinks along with the meal.",
                    timestamp = Timestamp(Date()),
                    items = listOf(
                        OrderDetailItem("Paneer Tikka Combo", 1, "Less spicy", 320.0, "Veg"),
                        OrderDetailItem("Butter Naan", 3, "Apply extra butter", 80.0, "Veg"),
                        OrderDetailItem("Coca Cola Can", 2, "Serve chilled with ice packs", 50.0, "Veg")
                    )
                )
            }
            else -> {
                // Fallback default catch-all order to keep things compiling smooth for any incoming ID click
                KitchenOrderDetailData(
                    orderId = orderId,
                    tableName = "Table - 10",
                    status = "New",
                    orderNote = "Customer requested quick service. Pack leftovers safely.",
                    timestamp = Timestamp(Date()),
                    items = listOf(
                        OrderDetailItem("Veg Hakka Noodles", 2, "Add extra green chilies", 200.0, "Veg"),
                        OrderDetailItem("Manchurian Gravy", 1, "", 180.0, "Veg"),
                        OrderDetailItem("Sweet Corn Soup", 1, "Serve piping hot", 120.0, "Veg")
                    )
                )
            }
        }

        // 2. Instantly send out the generated static ticket to satisfy your UI flow collector
        trySend(staticMockOrder)

        /* ⚠️ NOTE FOR WAITER FLOW PRODUCTION INTEGRATION STAGE:
        Uncomment the live collection stream listener block below and wipe the local block above
        when you are ready to process live tickets captured from the waiter screens!

        val docRef = firestore.collection("orders").document(orderId)
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore real-time snapshot transfer failure", error)
                close(error)
                return@addSnapshotListener
            }

            val orderData = snapshot?.toObject(KitchenOrderDetailData::class.java)?.copy(orderId = snapshot.id)
            trySend(orderData)
        }

        awaitClose { listenerRegistration.remove() }
        */

        // Keep block alive for manual text row visibility tests
        awaitClose { Log.d(TAG, "getLiveOrderDetails: Mock data stream loop channel closed.") }
    }

    /**
     * Simulates updating structural ticket tracking keys locally via Logcat outputs
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        Log.i(TAG, "updateOrderStatus: Mock cloud write sequence executed. Order [$orderId] fields altered to -> '$newStatus'")

        // Simulating the server write background suspension lag cycle cleanly
        kotlinx.coroutines.delay(200)

        /* PRODUCTION ENHANCEMENT RUN AT INTEGRATION STAGE:
        firestore.collection("orders")
            .document(orderId)
            .update("status", newStatus)
            .await()
        */
    }
}