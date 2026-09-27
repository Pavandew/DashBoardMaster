package com.restroone.app.manager_single_res_dash.uistate

import com.restroone.app.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel

sealed class RestaurantDetailsUiState {
    object Loading : RestaurantDetailsUiState()
    data class Success(val data: RegistrationDataModel) : RestaurantDetailsUiState()
    data class Error(val message: String) : RestaurantDetailsUiState()
}
