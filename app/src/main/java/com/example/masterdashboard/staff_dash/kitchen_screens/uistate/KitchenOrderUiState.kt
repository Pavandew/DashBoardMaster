package com.example.masterdashboard.staff_dash.kitchen_screens.uistate

import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData

sealed interface KitchenOrderUiState {
    object Loading : KitchenOrderUiState
    data class Success(
        val orders: List<KitchenOrderDetailData>,
        val filters: List<TableFilterData> = emptyList()
    ) : KitchenOrderUiState
    data class Error(val exception: Throwable) : KitchenOrderUiState
}