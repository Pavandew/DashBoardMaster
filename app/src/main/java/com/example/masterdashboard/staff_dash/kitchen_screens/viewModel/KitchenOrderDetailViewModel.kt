package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenOrderDetailsRepository
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class KitchenOrderDetailViewModel(
    private val repository: KitchenOrderDetailsRepository = KitchenOrderDetailsRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "KitchenOrderDetailVM"
    }

    private val _detailUiState = MutableStateFlow<KitchenOrderDetailUiState>(KitchenOrderDetailUiState.Loading)
    val detailUiState: StateFlow<KitchenOrderDetailUiState> = _detailUiState.asStateFlow()

    private val _statusUpdateAction = MutableStateFlow<Result<String>?>(null)
    val statusUpdateAction: StateFlow<Result<String>?> = _statusUpdateAction.asStateFlow()

    /**
     * Connects a dedicated real-time listener subscription framework to a specific order snapshot document
     */
    fun loadOrderDetails(orderId: String) {
        Log.i(TAG, "loadOrderDetails: Initializing real-time Firestore stream query link sequence for orderId: $orderId")
        _detailUiState.value = KitchenOrderDetailUiState.Loading

        viewModelScope.launch {
            repository.getLiveOrderDetails(orderId)
                .catch { exception ->
                    Log.e(TAG, "loadOrderDetails: Critical snapshot listener transmission exception caught for orderId: $orderId", exception)
                    _detailUiState.value = KitchenOrderDetailUiState.Error(exception)
                }
                .collect { detailedData ->
                    if (detailedData != null) {
                        Log.d(TAG, "loadOrderDetails: Live update parsed successfully. Table: ${detailedData.tableName}, Status: ${detailedData.status}, Total Items Pack: ${detailedData.items.size}")
                        _detailUiState.value = KitchenOrderDetailUiState.Success(detailedData)
                    } else {
                        Log.w(TAG, "loadOrderDetails: Query complete but document snapshot data returned null (Order may have been deleted or missing) for orderId: $orderId")
                        _detailUiState.value = KitchenOrderDetailUiState.Error(Exception("Order ticket not found or deleted."))
                    }
                }
        }
    }

    /**
     * Mutation engine handler to change the current operational ticket pipeline step (Accept, Reject, Finish)
     */
    fun updateTicketStatus(orderId: String, targetStatus: String) {
        Log.i(TAG, "updateTicketStatus: Request received to alter payload status properties. Target orderId: $orderId, Requested New State: $targetStatus")

        viewModelScope.launch {
            try {
                repository.updateOrderStatus(orderId, targetStatus)
                Log.d(TAG, "updateTicketStatus: Cloud data field write complete. Firestore status updated to '$targetStatus' for orderId: $orderId")
                _statusUpdateAction.value = Result.success(targetStatus)
            } catch (e: Exception) {
                Log.e(TAG, "updateTicketStatus: Critical error caught while executing status update collection transaction for orderId: $orderId", e)
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    /**
     * Clean out status mutation side-effect tokens to prevent recurring toast messaging events
     */
    fun resetStatusActionToken() {
        Log.d(TAG, "resetStatusActionToken: Wiping status mutation action channel event token references.")
        _statusUpdateAction.value = null
    }
}