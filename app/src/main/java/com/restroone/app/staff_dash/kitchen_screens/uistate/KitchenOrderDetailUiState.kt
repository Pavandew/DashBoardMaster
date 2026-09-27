package com.restroone.app.staff_dash.kitchen_screens.uistate

import com.restroone.app.staff_dash.kitchen_screens.model.KitchenOrderDetailData

sealed interface KitchenOrderDetailUiState {
    object Loading : KitchenOrderDetailUiState
    data class Success(val orderDetails: KitchenOrderDetailData) : KitchenOrderDetailUiState
    data class Error(val exception: Throwable) : KitchenOrderDetailUiState
}