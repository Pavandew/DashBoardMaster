package com.example.masterdashboard.staff_dash.billing_screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.repo.CashierBillingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CashierBillSummaryViewModel(
    private val repository: CashierBillingRepository = CashierBillingRepository()
) : ViewModel() {

    private val _orderData = MutableStateFlow<ResourceUiState<CashierBillingOrderModel>>(ResourceUiState.Idle)
    val orderData: StateFlow<ResourceUiState<CashierBillingOrderModel>> = _orderData.asStateFlow()

    fun fetchOrderDetails(docPath: String) {
        viewModelScope.launch {
            repository.fetchOrderDetails(docPath).collect { resource ->
                _orderData.value = resource
            }
        }
    }
}