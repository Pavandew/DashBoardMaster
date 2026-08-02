package com.example.masterdashboard.staff_dash.billing_screens.uiState

import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel

sealed interface CashierBillingUiState {
    object Loading : CashierBillingUiState
    data class Success(
        val orders: List<CashierBillingOrderModel> = emptyList(),
        val selectedFilter: String = "All"
    ) : CashierBillingUiState
    data class Error(val message: String) : CashierBillingUiState
}