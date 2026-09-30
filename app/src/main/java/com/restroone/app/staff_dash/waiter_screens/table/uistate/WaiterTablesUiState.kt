package com.restroone.app.staff_dash.waiter_screens.table.uistate

import com.restroone.app.staff_dash.waiter_screens.table.models.TableCardData
import com.restroone.app.staff_dash.waiter_screens.table.models.TableFilterData

data class WaiterTablesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedFloorId: String = "ALL_FLOORS",
    val floors: List<TableFilterData> = emptyList(),
    val tables: List<TableCardData> = emptyList(),
    val errorMessage: String? = null
)