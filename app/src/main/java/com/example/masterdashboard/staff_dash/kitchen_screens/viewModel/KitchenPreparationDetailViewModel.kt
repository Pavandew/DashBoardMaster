package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenPreparationDetailRepository
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KitchenPreparationDetailViewModel(
    private val repository: KitchenPreparationDetailRepository = KitchenPreparationDetailRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "KitchenPrepDetailVM"
    }

    private val _rawOrderData = MutableStateFlow<KitchenOrderDetailData?>(null)
    val detailUiState: StateFlow<KitchenOrderDetailUiState> = _rawOrderData.map { data ->
        if (data == null) {
            KitchenOrderDetailUiState.Loading
        } else {
            KitchenOrderDetailUiState.Success(data)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KitchenOrderDetailUiState.Loading)

    private val _statusUpdateAction = MutableStateFlow<Result<String>?>(null)
    val statusUpdateAction: StateFlow<Result<String>?> = _statusUpdateAction.asStateFlow()

    fun loadOrderDetails(docPath: String) {
        viewModelScope.launch {
            repository.listenToOrderDetails(docPath).collectLatest { updatedData ->
                _rawOrderData.value = updatedData
            }
        }
    }

    fun finalizeOrderToServe(docPath: String) {
        viewModelScope.launch {
            try {
                repository.updateOrderStatusToReady(docPath)
                _statusUpdateAction.value = Result.success("Order Ready")
            } catch (e: Exception) {
                Log.e(TAG, "finalizeOrderToServe failed", e)
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    /**
     * Marks a set of items as ready by incrementing their readyQuantity in Firestore.
     */
    fun markItemsAsReady(docPath: String, itemsToMark: List<OrderDetailItem>) {
        val currentData = _rawOrderData.value ?: return
        
        viewModelScope.launch {
            try {
                val updatedItemsList = currentData.items.map { originalItem ->
                    val isToMark = itemsToMark.any { it.itemId == originalItem.itemId }
                    if (isToMark) {
                        // Mark as fully ready for now as per simple request
                        originalItem.copy(readyQuantity = originalItem.quantity)
                    } else {
                        originalItem
                    }
                }

                // Convert to Map for Firestore
                val itemsMap = updatedItemsList.map { item ->
                    mapOf(
                        "itemId" to item.itemId,
                        "itemName" to item.itemName,
                        "quantity" to item.quantity,
                        "orderedQuantity" to item.orderedQuantity,
                        "readyQuantity" to item.readyQuantity,
                        "price" to item.price,
                        "rowTotal" to item.rowTotal,
                        "category" to item.category,
                        "itemNote" to item.itemNote
                    )
                }

                repository.updateItemsAsReady(docPath, itemsMap)
                
                // If all items are now ready, we could automatically finalize the order,
                // but let's leave that to the chef for now to avoid confusion.
                
                _statusUpdateAction.value = Result.success("Items Prepared")
            } catch (e: Exception) {
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    fun resetStatusActionToken() {
        _statusUpdateAction.value = null
    }
}