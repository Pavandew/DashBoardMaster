package com.restroone.app.manager_single_res_dash.uistate

import com.restroone.app.manager_single_res_dash.models.CustomerModel

data class CustomerUiState(
    val customers: List<CustomerModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)