package com.example.masterdashboard.staff_dash.waiter_screens.order.views.repo

import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.views.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ActiveOrdersRepository {
    companion object {
        private const val TAG = "ActiveOrdersRepository"
    }

    // simulates an API call fetching category filters
    fun fetchLiveActiveOrders(): Flow<ResourceUiState<List<ActiveOrderCardData>>> = flow {
        emit(ResourceUiState.Loading)

        try {
            delay(300)
            val dummyOrders = listOf(
                ActiveOrderCardData("ORD-1256", "T-02", 2, "11:30 AM", ActiveOrderStatus.PREPARING),
                ActiveOrderCardData("ORD-1258", "T-05", 4, "11:30 AM", ActiveOrderStatus.PREPARING),
                ActiveOrderCardData("ORD-1259", "T-08", 3, "11:25 AM", ActiveOrderStatus.READY),
                ActiveOrderCardData("ORD-1255", "T-01", 2, "11:10 AM", ActiveOrderStatus.SERVED),
                ActiveOrderCardData("ORD-1260", "T-11", 3, "11:40 AM", ActiveOrderStatus.PREPARING)
            )

            emit(ResourceUiState.Success(dummyOrders))
        } catch (e: Exception) {
            emit(ResourceUiState.Error("Failed to fetch active orders: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

}