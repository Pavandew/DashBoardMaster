package com.example.masterdashboard.staff_dash.waiter_screens.order.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.notifications.AppNotificationHelper
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderDetailExpansionUiState
import com.example.masterdashboard.staff_dash.waiter_screens.order.repo.OrderDetailRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.notifications.NotificationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class OrderDetailViewModel(
    private val repository: OrderDetailRepository
) : ViewModel() {

    companion object {
        private const val TAG = "Order_Detail_Debug"
    }

    class OrderDetailViewModelFactory(private val repository: OrderDetailRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return OrderDetailViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel configuration")
        }
    }

    private val _uiState = MutableStateFlow(OrderDetailExpansionUiState())
    val uiState: StateFlow<OrderDetailExpansionUiState> = _uiState.asStateFlow()

    fun loadOrderSpecifications(
        managerId: String,
        orderId: String,
        preloadedTableName: String,
        preloadedStatus: String,
        preloadedTime: String
    ) {
        val initialStatus = try {
            ActiveOrderStatus.valueOf(preloadedStatus.uppercase())
        } catch (e: Exception) {
            ActiveOrderStatus.PREPARING
        }

        // Apply preloaded arguments immediately to prevent blank UI while fetching items
        _uiState.update {
            it.copy(
                isLoading = true,
                orderId = orderId,
                tableName = preloadedTableName,
                status = initialStatus,
                timeStamp = preloadedTime
            )
        }

        repository.fetchDetailedTicket(managerId, orderId, preloadedTableName).onEach { resource ->
            when (resource) {
                is ResourceUiState.Loading -> {
                    Log.d(TAG, "🏗️ [VIEWMODEL] Loading order details...")
                }
                is ResourceUiState.Success -> {
                    Log.i(TAG, "🏗️ [VIEWMODEL] Order details loaded successfully!")
                    _uiState.value = resource.data
                }
                is ResourceUiState.Error -> {
                    Log.e(TAG, "🏗️ [VIEWMODEL] Error loading details: ${resource.message}")
                    _uiState.update { it.copy(isLoading = false, errorMessage = resource.message) }
                }
                ResourceUiState.Idle -> {}
            }
        }.launchIn(viewModelScope)
    }

    fun finalizeOrderAsServed(
        managerId: String,
        floorId: String,
        tableId: String,
        orderDocId: String,
        onCompletionSuccess: () -> Unit
    ) {
        updateStatus(repository.updateOrderStatusToServed(managerId, floorId, tableId, orderDocId), onCompletionSuccess)
    }

    fun finalizeOrderAsBilling(
        managerId: String,
        floorId: String,
        tableId: String,
        orderDocId: String,
        onCompletionSuccess: () -> Unit
    ) {
        val tableName = uiState.value.tableName
        val orderId = uiState.value.orderId

        updateStatus(repository.updateOrderStatusToBilling(managerId, floorId, tableId, orderDocId)) {
            Log.d(TAG, "finalizeOrderAsBilling: Status updated to BILLING. Triggering notification logic.")
            
            val isCounter = tableId.isEmpty() || tableId == "COUNTER_ORDER" || tableId == "N/A" || floorId.isEmpty() || floorId == "N/A"
            val docPath = if (isCounter) {
                "users/$managerId/active_orders/$orderDocId"
            } else {
                "users/$managerId/res_floors/$floorId/floor_tables/$tableId/active_orders/$orderDocId"
            }

            // CRITICAL: Use a separate launch to ensure notification is attempted
            // before we potentially destroy the ViewModel/Fragment scope.
            viewModelScope.launch {
                try {
                    val tokens = repository.getCashierTokens(managerId)
                    Log.d(TAG, "finalizeOrderAsBilling: Found ${tokens.size} cashier tokens. Triggering notification.")
                    
                    // ALWAYS call dispatcher to save to history, even if tokens are empty.
                    AppNotificationHelper.notifyCashierOfBillRequest(tokens, tableName, orderId, managerId, docPath)
                } catch (e: Exception) {
                    Log.e(TAG, "finalizeOrderAsBilling: Notification failed", e)
                } finally {
                    onCompletionSuccess()
                }
            }
        }
    }

    private fun updateStatus(
        flow: kotlinx.coroutines.flow.Flow<ResourceUiState<Boolean>>,
        onCompletionSuccess: () -> Unit
    ) {
        flow.onEach { resource ->
            when (resource) {
                is ResourceUiState.Loading -> _uiState.update { it.copy(isLoading = true) }
                is ResourceUiState.Success -> {
                    // Note: We don't update local status here because we want the refresh from repo to handle it
                    // but for instant feedback we can:
                    _uiState.update { it.copy(isLoading = false) }
                    onCompletionSuccess()
                }
                is ResourceUiState.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = resource.message) }
                }
                ResourceUiState.Idle -> {}
            }
        }.launchIn(viewModelScope)
    }
}