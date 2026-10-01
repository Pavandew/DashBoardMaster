package com.restroone.app.staff_dash.billing_screens.uiState

import com.restroone.app.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.restroone.app.staff_dash.waiter_screens.table.models.TableFilterData

data class CashierBillingUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val orders: List<CashierBillingOrderModel> = emptyList(),
    val selectedFilter: String = "All",
    val filters: List<TableFilterData> = emptyList(),
    val errorMessage: String? = null
)
