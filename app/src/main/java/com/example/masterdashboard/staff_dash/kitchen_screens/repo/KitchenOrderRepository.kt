package com.example.masterdashboard.staff_dash.kitchen_screens.repo

import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

class KitchenOrderRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /**
     * Emits dummy data instantly to verify your floating card list states,
     * then falls back to listening to real-time firestore changes if desired.
     */
    fun getRealtimeKitchenOrderDetailDatas(): Flow<List<KitchenOrderDetailData>> = callbackFlow {

        // FIX: Removed 'itemsSummary' and replaced it with type-safe 'items' lists + 'orderNote' strings
        val dummyOrders = listOf(
            KitchenOrderDetailData(
                orderId = "ORD9841",
                tableName = "Table 04",
                status = "New",
                orderNote = "Deliver everything together. Make sure the burgers have extra cheese slices.",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 2 * 60 * 1000)), // 2 mins ago
                items = listOf(
                    OrderDetailItem("Crispy Chicken Burger", 2, "No onions", 250.0, "Non-Veg"),
                    OrderDetailItem("Loaded Cheesy Fries", 1, "Extra spicy", 150.0, "Veg"),
                    OrderDetailItem("Classic Cold Coffee", 1, "", 120.0, "Veg")
                )
            ),
            KitchenOrderDetailData(
                orderId = "ORD8722",
                tableName = "Table 12",
                status = "Preparing",
                orderNote = "Appetizers first please. Drinks along with the meal.",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 12 * 60 * 1000)), // 12 mins ago
                items = listOf(
                    OrderDetailItem("Paneer Tikka Combo", 1, "Less spicy", 320.0, "Veg"),
                    OrderDetailItem("Butter Naan", 3, "Apply extra butter", 80.0, "Veg"),
                    OrderDetailItem("Coca Cola Can", 2, "Serve chilled", 50.0, "Veg")
                )
            ),
            KitchenOrderDetailData(
                orderId = "ORD6510",
                tableName = "Table 02",
                status = "Preparing",
                orderNote = "",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 25 * 60 * 1000)), // 25 mins ago
                items = listOf(
                    OrderDetailItem("Veg Hakka Noodles", 2, "Add extra green chilies", 200.0, "Veg"),
                    OrderDetailItem("Sweet Corn Soup", 1, "Serve piping hot", 120.0, "Veg")
                )
            ),
            KitchenOrderDetailData(
                orderId = "ORD4301",
                tableName = "Table 09",
                status = "Ready",
                orderNote = "Quick service requested",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 32 * 60 * 1000)), // 32 mins ago
                items = listOf(
                    OrderDetailItem("Sizzler Pack", 1, "", 450.0, "Non-Veg"),
                    OrderDetailItem("Chocolate Shake", 1, "Extra chocolate syrup", 140.0, "Veg")
                )
            ),
            KitchenOrderDetailData(
                orderId = "ORD1290",
                tableName = "Table 07",
                status = "Completed",
                orderNote = "",
                timestamp = Timestamp(Date(System.currentTimeMillis() - 60 * 60 * 1000)), // 1 hour ago
                items = listOf(
                    OrderDetailItem("Classic Cold Coffee", 2, "", 120.0, "Veg")
                )
            )
        )

        // Instantly send out the dummy list to verify UI rendering layout density
        trySend(dummyOrders)

        /* NOTE FOR PRODUCTION STAGE:
        Uncomment the block below and remove the trySend(dummyOrders) above
        when you are ready to switch from local mock data to your live Firestore collections!

        val listenerRegistration = firestore.collection("orders")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val ordersList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(KitchenOrderDetailData::class.java)?.copy(orderId = doc.id)
                } ?: emptyList()

                trySend(ordersList)
            }

        awaitClose { listenerRegistration.remove() }
        */

        // Keeps stream scope alive for checking view state switches locally
        awaitClose { /* No-Op for manual testing */ }
    }
}