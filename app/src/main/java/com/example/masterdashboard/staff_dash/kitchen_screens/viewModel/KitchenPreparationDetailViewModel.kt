package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.notifications.AppNotificationHelper
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.model.OrderDetailItem
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenPreparationDetailRepository
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import com.example.masterdashboard.notifications.NotificationHelper
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
        val currentData = _rawOrderData.value ?: return
        viewModelScope.launch {
            try {
                repository.updateOrderStatusToReady(docPath)
                _statusUpdateAction.value = Result.success("Order Ready")

                // Notify Waiter
                notifyWaiter(currentData)
            } catch (e: Exception) {
                Log.e(TAG, "finalizeOrderToServe failed", e)
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    private suspend fun notifyWaiter(data: KitchenOrderDetailData) {
        Log.d(TAG, "notifyWaiter: Attempting to notify waiter for order ${data.orderId}. WaiterID: '${data.waiterId}', RestaurantID: '${data.restaurantId}'")
        
        if (data.waiterId.isEmpty()) {
            Log.w(TAG, "notifyWaiter: waiterId is empty. Notification cannot be sent.")
            return
        }

        // Fetch token for PUSH
        val token = repository.getWaiterToken(data.restaurantId, data.waiterId)
        
        // ALWAYS call dispatcher. It will save the record to history even if token is null.
        AppNotificationHelper.notifyWaiterOrderReady(
            waiterToken = token,
            tableName = data.tableName,
            orderId = data.orderId,
            managerId = data.restaurantId,
            waiterId = data.waiterId,
            orderDocPath = data.docPath
        )
        
        if (token == null) {
            Log.d(TAG, "notifyWaiter: No FCM token found for waiter ${data.waiterId}. Record saved to history only.")
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
                        // Mark as fully ready and update status
                        originalItem.copy(
                            readyQuantity = originalItem.quantity,
                            itemStatus = "READY"
                        )
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
                        "itemNote" to item.itemNote,
                        "itemStatus" to item.itemStatus
                    )
                }

                repository.updateItemsAsReady(docPath, itemsMap)
                
                // Auto-Finalize Check: If all items in this order are now ready,
                // automatically move the order to READY status and notify the waiter.
                val allReady = updatedItemsList.all { it.readyQuantity >= it.quantity || it.quantity <= 0 }
                
                if (allReady) {
                    Log.i(TAG, "markItemsAsReady: All items are ready. Auto-finalizing order status.")
                    finalizeOrderToServe(docPath)
                } else {
                    _statusUpdateAction.value = Result.success("Items Prepared")
                }
            } catch (e: Exception) {
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    fun resetStatusActionToken() {
        _statusUpdateAction.value = null
    }
}