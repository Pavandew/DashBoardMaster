package com.example.masterdashboard.manager_single_res_dash.uistate

import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel

sealed class RestaurantDetailsUiState {
    object Loading : RestaurantDetailsUiState()
    data class Success(val data: RegistrationDataModel) : RestaurantDetailsUiState()
    data class Error(val message: String) : RestaurantDetailsUiState()
}
