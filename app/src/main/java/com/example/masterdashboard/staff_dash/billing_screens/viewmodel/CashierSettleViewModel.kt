package com.example.masterdashboard.staff_dash.billing_screens.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.notifications.AppNotificationHelper
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.repo.CashierBillingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CashierSettleViewModel(
    private val repository: CashierBillingRepository = CashierBillingRepository()
) : ViewModel() {

    private val _activeBillingOrder = MutableStateFlow<CashierBillingOrderModel?>(null)
    val activeBillingOrder: StateFlow<CashierBillingOrderModel?> = _activeBillingOrder.asStateFlow()

    private val _settleState = MutableStateFlow<ResourceUiState<String>>(ResourceUiState.Idle)
    val settleState: StateFlow<ResourceUiState<String>> = _settleState.asStateFlow()

    private var selectedPaymentMode = "Cash"
    private var discountAmount = 0.0

    fun setOrder(order: CashierBillingOrderModel) {
        _activeBillingOrder.value = order
        // If items are missing, load full details automatically
        if (order.items.isEmpty() && order.docPath.isNotEmpty()) {
            loadFullOrderDetails(order.docPath)
        }
    }

    private fun loadFullOrderDetails(docPath: String) {
        viewModelScope.launch {
            repository.fetchOrderDetails(docPath).collect { resource ->
                if (resource is ResourceUiState.Success) {
                    _activeBillingOrder.value = resource.data
                }
            }
        }
    }

    fun setSelectedPaymentMode(mode: String) {
        selectedPaymentMode = mode
    }

    fun getSelectedPaymentMode(): String = selectedPaymentMode

    fun applyDiscount(amount: Double) {
        discountAmount = amount
        _activeBillingOrder.value = _activeBillingOrder.value?.copy(
            discountAmount = amount,
            grandTotal = (_activeBillingOrder.value?.subtotal ?: 0.0) + 
                         (_activeBillingOrder.value?.taxAmount ?: 0.0) - amount
        )
    }

    fun settleAndCompleteOrder(managerId: String) {
        val order = _activeBillingOrder.value ?: return
        viewModelScope.launch {
            repository.settleOrder(order, selectedPaymentMode, discountAmount).collect { status ->
                _settleState.value = status
                
                if (status is ResourceUiState.Success) {
                    try {
                        val completedDocPath = "users/$managerId/completed_orders/${order.orderId}"
                        // Notify Manager (Revenue), Waiter (Table Free), and Billing (Log)
                        AppNotificationHelper.notifyPaymentSuccess(
                            managerId = managerId,
                            tableName = order.tableName,
                            orderId = order.orderId,
                            amount = order.grandTotal,
                            waiterId = order.waiterId,
                            orderDocPath = completedDocPath
                        )
                    } catch (e: Exception) {
                        Log.e("CashierSettleVM", "Error sending notifications post payment", e)
                    }
                }
            }
        }
    }

    fun confirmPickup() {
        val order = _activeBillingOrder.value ?: return
        viewModelScope.launch {
            repository.confirmTakeawayPickup(order).collect {
                _settleState.value = it
            }
        }
    }
}