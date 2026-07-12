package com.example.masterdashboard.staff_dash.kitchen_screens.uistate

import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData

sealed interface KitchenOrderDetailUiState {
    object Loading : KitchenOrderDetailUiState
    data class Success(val orderDetails: KitchenOrderDetailData) : KitchenOrderDetailUiState
    data class Error(val exception: Throwable) : KitchenOrderDetailUiState
}