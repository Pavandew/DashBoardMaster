package com.restroone.app.staff_dash.kitchen_screens.uistate

import com.restroone.app.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.restroone.app.staff_dash.waiter_screens.table.models.TableFilterData

data class KitchenOrderUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val orders: List<KitchenOrderDetailData> = emptyList(),
    val filters: List<TableFilterData> = emptyList(),
    val errorMessage: String? = null
)
