package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.CartSummaryState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrderTakingViewModel(private val repository: OrderTakingRepository) : ViewModel() {

    companion object {
        private const val TAG = "Order_Flow_Debug"
    }

    class OrderViewModelFactory(private val repository: OrderTakingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderTakingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return OrderTakingViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel configuration")
        }
    }

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    var originalFoodList: List<FoodItemData> = emptyList()
        private set

    init {
        Log.d(TAG, "🏗️ [VIEWMODEL] OrderTakingViewModel instance successfully initialized.")
    }

    fun loadMenuData(managerId: String?) {
        if (managerId.isNullOrEmpty()) {
            Log.w(TAG, "🏗️ [VIEWMODEL] loadMenuData called with a null or empty Manager ID.")
            _uiState.update { it.copy(errorMessage = "Session mismatch error", isLoading = false) }
            return
        }

        Log.d(TAG, "🏗️ [VIEWMODEL] Fetching custom menu courses layout maps for ID: $managerId")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Fetch Live Categories from Firestore
        viewModelScope.launch {
            repository.getMenuCategories(managerId)
                .catch { exception ->
                    Log.e(TAG, "🏗️ [VIEWMODEL] Error inside categories stream collection pass", exception)
                    _uiState.update { it.copy(errorMessage = exception.message, isLoading = false) }
                }
                .collect { dynamicCategories ->
                    _uiState.update { state ->
                        state.copy(categories = dynamicCategories, isLoading = false)
                    }
                }
        }

        // Fetch Live Nested Menu Food Items from Firestore
        viewModelScope.launch {
            repository.getFoodMenu(managerId)
                .catch { exception ->
                    Log.e(TAG, "🏗️ [VIEWMODEL] Error inside nested food menu items stream collection pass", exception)
                    _uiState.update { it.copy(errorMessage = exception.message, isLoading = false) }
                }
                .collect { dynamicFoodItems ->
                    originalFoodList = dynamicFoodItems
                    val summary = calculateCartTotalsInternal(dynamicFoodItems)

                    _uiState.update { state ->
                        state.copy(menuItems = dynamicFoodItems, cartSummary = summary, isLoading = false)
                    }
                }
        }
    }

    fun updateItemQuantity(foodId: String, increase: Boolean) {
        Log.d(TAG, "🏗️ [VIEWMODEL] updateItemQuantity execution -> ID: $foodId, Action Increase: $increase")
        _uiState.update { currentState ->
            // Update the master list (originalFoodList) so we don't lose items when UI is filtered by category
            val updatedOriginal = originalFoodList.map { item ->
                if (item.id == foodId) {
                    val newQty = if (increase) item.currentQuantity + 1 else maxOf(0, item.currentQuantity - 1)
                    item.copy(currentQuantity = newQty)
                } else item
            }

            originalFoodList = updatedOriginal

            // Respect the currently selected category when producing the UI menu list
            val selectedCategoryId = currentState.categories.find { it.isSelected }?.id ?: "ALL_ITEMS"
            val updatedMenuItems = if (selectedCategoryId == "ALL_ITEMS") {
                updatedOriginal
            } else {
                updatedOriginal.filter { it.categoryId == selectedCategoryId }
            }

            val summary = calculateCartTotalsInternal(updatedOriginal)
            currentState.copy(menuItems = updatedMenuItems, cartSummary = summary)
        }
    }

    fun clearCart() {
        Log.d(TAG, "🏗️ [VIEWMODEL] clearCart operation triggered.")
        _uiState.update { currentState ->
            // Reset quantities on the master original list so filters retain full data
            val resetOriginal = originalFoodList.map { item ->
                item.copy(currentQuantity = 0)
            }
            originalFoodList = resetOriginal

            val blankSummary = CartSummaryState(totalItems = 0, totalPrice = 0)

            val selectedCategoryId = currentState.categories.find { it.isSelected }?.id ?: "ALL_ITEMS"
            val updatedMenuItems = if (selectedCategoryId == "ALL_ITEMS") {
                resetOriginal
            } else {
                resetOriginal.filter { it.categoryId == selectedCategoryId }
            }

            currentState.copy(menuItems = updatedMenuItems, cartSummary = blankSummary)
        }
    }

    fun selectCategory(categoryId: String) {
        Log.d(TAG, "🏗️ [VIEWMODEL] selectCategory clicked targeting category filter value: $categoryId")
        _uiState.update { currentState ->
            val updatedCategories = currentState.categories.map {
                it.copy(isSelected = it.id == categoryId)
            }

            // Filters based on our locally tracked original list structure
            val filteredMenuItems = if (categoryId == "ALL_ITEMS") {
                originalFoodList
            } else {
                originalFoodList.filter { it.categoryId == categoryId }
            }

            currentState.copy(categories = updatedCategories, menuItems = filteredMenuItems)
        }
    }

    private fun calculateCartTotalsInternal(items: List<FoodItemData>): CartSummaryState {
        val activeSelections = items.filter { it.currentQuantity > 0 }
        val count = activeSelections.sumOf { it.currentQuantity }
        val total = activeSelections.sumOf { it.price * it.currentQuantity }
        return CartSummaryState(count, total)
    }
}