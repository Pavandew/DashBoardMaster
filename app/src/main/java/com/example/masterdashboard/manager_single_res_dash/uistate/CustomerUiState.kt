package com.example.masterdashboard.manager_single_res_dash.uistate

import com.example.masterdashboard.manager_single_res_dash.models.CustomerModel

data class CustomerUiState(
    val customers: List<CustomerModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)