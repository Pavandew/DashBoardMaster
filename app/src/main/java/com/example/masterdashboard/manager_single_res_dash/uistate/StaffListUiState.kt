package com.example.masterdashboard.manager_single_res_dash.uistate

import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel

sealed interface StaffListUiState {
    object Loading: StaffListUiState
    data class Success(val list: List<StaffDataModel>) : StaffListUiState
    object Empty: StaffListUiState
    data class Error(val message: String) : StaffListUiState

}

sealed interface StaffDetailUiState {
    object Loading : StaffDetailUiState
    data class Success(val staff: StaffDataModel) : StaffDetailUiState
    data class Error(val message: String) : StaffDetailUiState
}

