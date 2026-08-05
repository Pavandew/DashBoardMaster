package com.example.masterdashboard.manager_single_res_dash.uistate

import com.example.masterdashboard.manager_single_res_dash.models.FloorDataModel
import com.example.masterdashboard.manager_single_res_dash.models.TableData

sealed interface TableUiState {
    object Loading: TableUiState
    data class Success(val list: List<FloorDataModel>) : TableUiState
    object Empty: TableUiState
    data class Error(val message: String) : TableUiState

}

sealed interface TableItemUiState {
    object Loading: TableItemUiState
    object Empty: TableItemUiState
    data class Success(
        val tables: List<TableData>,
        val totalCount: Int,
        val availableCount: Int,
        val occupiedCount: Int,
        val isRefreshing: Boolean = false
    ) : TableItemUiState

    data class Error(val message: String) : TableItemUiState
}

