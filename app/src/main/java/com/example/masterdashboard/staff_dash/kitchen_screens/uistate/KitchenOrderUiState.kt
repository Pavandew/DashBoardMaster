package com.example.masterdashboard.staff_dash.kitchen_screens.uistate

import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData


sealed interface KitchenOrderUiState {
    object Loading : KitchenOrderUiState
    data class Success(val orders: List<KitchenOrderDetailData>) : KitchenOrderUiState
    data class Error(val exception: Throwable) : KitchenOrderUiState
}