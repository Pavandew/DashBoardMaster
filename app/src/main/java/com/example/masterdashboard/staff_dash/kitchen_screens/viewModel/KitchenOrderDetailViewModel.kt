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
     * Connects a dedicated real-time listener to a specific order document using its full path.
     */
    fun loadOrderDetails(docPath: String) {
        Log.i(TAG, "loadOrderDetails: Initializing real-time Firestore stream for path: $docPath")
        _detailUiState.value = KitchenOrderDetailUiState.Loading

        viewModelScope.launch {
            repository.getLiveOrderDetails(docPath)
                .catch { exception ->
                    Log.e(TAG, "loadOrderDetails: Error for path: $docPath", exception)
                    _detailUiState.value = KitchenOrderDetailUiState.Error(exception)
                }
                .collect { detailedData ->
                    if (detailedData != null) {
                        _detailUiState.value = KitchenOrderDetailUiState.Success(detailedData)
                    } else {
                        _detailUiState.value = KitchenOrderDetailUiState.Error(Exception("Order ticket not found."))
                    }
                }
        }
    }

    /**
     * Updates the ticket status in Firestore.
     */
    fun updateTicketStatus(docPath: String, targetStatus: String, reason: String = "") {
        Log.i(TAG, "updateTicketStatus: Request for path: $docPath, New Status: $targetStatus")

        viewModelScope.launch {
            try {
                repository.updateOrderStatus(docPath, targetStatus, reason)
                _statusUpdateAction.value = Result.success(targetStatus)
            } catch (e: Exception) {
                Log.e(TAG, "updateTicketStatus: Error for path: $docPath", e)
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    /**
     * Rejects specific items from the order and updates Firestore.
     */
    fun rejectSpecificItems(
        docPath: String,
        remainingItems: List<com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem>,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                // Recalculate totals
                val newSubtotal = remainingItems.sumOf { it.price.toDouble() * it.quantity }
                val newGst = newSubtotal * 0.05
                val newGrandTotal = newSubtotal + newGst

                // Convert items to Map for Firestore
                val itemsMap = remainingItems.map { item ->
                    mapOf(
                        "itemId" to item.itemId,
                        "itemName" to item.itemName,
                        "quantity" to item.quantity,
                        "orderedQuantity" to item.orderedQuantity,
                        "price" to item.price,
                        "rowTotal" to (item.price * item.quantity),
                        "category" to item.category,
                        "itemNote" to item.itemNote
                    )
                }

                repository.updateOrderWithRejectedItems(
                    docPath,
                    itemsMap,
                    newSubtotal,
                    newGst,
                    newGrandTotal,
                    reason
                )

                _statusUpdateAction.value = Result.success("Items Rejected: $reason")
            } catch (e: Exception) {
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    fun resetStatusActionToken() {
        _statusUpdateAction.value = null
    }
}