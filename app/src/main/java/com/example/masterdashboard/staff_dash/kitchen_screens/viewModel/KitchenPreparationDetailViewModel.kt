package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenPreparationDetailRepository
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderDetailUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KitchenPreparationDetailViewModel(
    private val repository: KitchenPreparationDetailRepository = KitchenPreparationDetailRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "KitchenWorkstationVM"
    }

    private val _rawOrderData = MutableStateFlow<KitchenOrderDetailData?>(null)
    private val _activeTabPosition = MutableStateFlow(0) // 0: All, 1: Veg, 2: Non-Veg, 3: Drinks

    // Combines the raw snapshot and tab index to provide type-safe filtered list states
    val detailUiState: StateFlow<KitchenOrderDetailUiState> = combine(_rawOrderData, _activeTabPosition) { data, tabIndex ->
        if (data == null) {
            KitchenOrderDetailUiState.Loading
        } else {
            val filteredItems = when (tabIndex) {
                1 -> data.items.filter { it.itemName.contains("Veg", ignoreCase = true) || it.itemName.contains("Paneer", ignoreCase = true) }
                2 -> data.items.filter { !it.itemName.contains("Veg", ignoreCase = true) && !it.itemName.contains("Drink", ignoreCase = true) && !it.itemName.contains("Coke", ignoreCase = true) && !it.itemName.contains("Paneer", ignoreCase = true) }
                3 -> data.items.filter { it.itemName.contains("Drink", ignoreCase = true) || it.itemName.contains("Coke", ignoreCase = true) || it.itemName.contains("Coffee", ignoreCase = true) }
                else -> data.items // "All"
            }
            KitchenOrderDetailUiState.Success(data.copy(items = filteredItems))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KitchenOrderDetailUiState.Loading)

    private val _statusUpdateAction = MutableStateFlow<Result<String>?>(null)
    val statusUpdateAction: StateFlow<Result<String>?> = _statusUpdateAction.asStateFlow()

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            repository.listenToOrderDetails(orderId).collectLatest { updatedData ->
                _rawOrderData.value = updatedData
            }
        }
    }

    fun setCategoryTabFilter(position: Int) {
        Log.d(TAG, "setCategoryTabFilter: Shifting category mapping filter reference to index slot: [$position]")
        _activeTabPosition.value = position
    }

    fun finalizeOrderToServe(orderId: String) {
        viewModelScope.launch {
            try {
                repository.updateOrderStatusToReady(orderId)
                _statusUpdateAction.value = Result.success("Ready")
            } catch (e: Exception) {
                Log.e(TAG, "finalizeOrderToServe: Mutation transaction failed for orderId: $orderId", e)
                _statusUpdateAction.value = Result.failure(e)
            }
        }
    }

    fun resetStatusActionToken() {
        _statusUpdateAction.value = null
    }
}