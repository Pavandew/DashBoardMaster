package com.example.masterdashboard.manager_single_res_dash.home.uistate

import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel

sealed interface StaffListUiState {
    object Loading: StaffListUiState
    data class Success(val list: List<StaffDataModel>) : StaffListUiState
    object Empty: StaffListUiState
    data class Error(val message: String) : StaffListUiState

}