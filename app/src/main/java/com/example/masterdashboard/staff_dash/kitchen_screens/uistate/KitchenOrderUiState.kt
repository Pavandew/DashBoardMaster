package com.example.masterdashboard.staff_dash.kitchen_screens.uistate


import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderData

sealed interface KitchenOrderUiState {
    object Loading : KitchenOrderUiState
    data class Success(val orders: List<KitchenOrderData>) : KitchenOrderUiState
    data class Error(val exception: Throwable) : KitchenOrderUiState
}