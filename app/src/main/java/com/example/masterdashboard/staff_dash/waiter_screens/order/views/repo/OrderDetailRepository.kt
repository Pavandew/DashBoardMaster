package com.example.masterdashboard.staff_dash.waiter_screens.order.views.repo

import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.OrderDetailExpansionUiState
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.OrderExpandedItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class OrderDetailRepository {

    fun fetchDetailedTicked(orderId: String): Flow<ResourceUiState<OrderDetailExpansionUiState>> =
        flow {
            emit(ResourceUiState.Loading)

            try {
                delay(400)

                val mockItems = listOf(
                    OrderExpandedItemData("f1", "Paneer Tikka", 1, 199, 199),
                    OrderExpandedItemData("f2", "Veg Manchurian", 2, 179, 358),
                    OrderExpandedItemData("f3", "Chicken Biryani", 1, 249, 249),
                    OrderExpandedItemData("f4", "Cold Coffee", 1, 119, 119)
                )

                val subtotal = mockItems.sumOf { it.totalPrice }
                val gst = subtotal * 0.05
                val grandTotal = subtotal + gst

                val detailPayload = OrderDetailExpansionUiState(
                    isLoading = false,
                    errorMessage = null,
                    orderId = orderId,
                    tableId = "T-05",
                    status = ActiveOrderStatus.PREPARING,
                    timeStamp = "11 May 2026, 11:30 AM",
                    items = mockItems,
                    subtotal = subtotal,
                    gstAmount = gst,
                    grandTotal = grandTotal
                )

                emit(ResourceUiState.Success(detailPayload))
            } catch (e: Exception) {
                emit(ResourceUiState.Error("Failed to fetch order details: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)

    // suspend logic workflow block to push the dynamic change up to database systems
    fun updateOrderStatusToServed(orderId: String): Flow<ResourceUiState<Boolean>> = flow {
        emit(ResourceUiState.Loading)

        try {
            delay(300)
            // Here you would normally call your API to update the order status in the backend
            // For this mock, we just simulate a successful update
            emit(ResourceUiState.Success(true))
        } catch (e: Exception) {
            emit(ResourceUiState.Error("Failed to update order status: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}