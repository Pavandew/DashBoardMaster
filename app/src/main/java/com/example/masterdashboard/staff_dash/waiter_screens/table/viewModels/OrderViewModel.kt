package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.CartSummaryState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    companion object {
        private const val TAG = "OrderViewModel"
    }

    class OrderViewModelFactory(private val repository: OrderRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return OrderViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel configuration")
        }
    }

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    var originalFoodList: List<FoodItemData> = emptyList()
        private set

    init {
        loadScreenContent()
    }

    fun loadScreenContent() {
        Log.d(TAG, "loadScreenContent: Fetching menu and categories...")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val categoriesDeferred = async { repository.getMenuCategories() }
                val foodItemsDeferred = async { repository.getFoodMenu() }

                val categories = categoriesDeferred.await().mapIndexed { index, category ->
                    if (index == 0) category.copy(isSelected = true) else category
                }
                val foodItems = foodItemsDeferred.await()

                Log.d(TAG, "loadScreenContent: Successfully fetched ${categories.size} categories and ${foodItems.size} items")

                originalFoodList = foodItems
                val summary = calculateCartTotalsInternal(foodItems)

                _uiState.update {
                    it.copy(
                        categories = categories,
                        menuItems = foodItems,
                        cartSummary = summary,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadScreenContent: Error fetching data", e)
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "An unexpected error occurred",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateItemQuantity(foodId: String, increase: Boolean) {
        Log.d(TAG, "updateItemQuantity: foodId=$foodId, increase=$increase")
        _uiState.update { currentState ->
            val updatedList = currentState.menuItems.map { item ->
                if (item.id == foodId) {
                    val newQty = if (increase) item.currentQuantity + 1 else maxOf(0, item.currentQuantity - 1)
                    item.copy(currentQuantity = newQty)
                } else item
            }
            originalFoodList = updatedList
            val summary = calculateCartTotalsInternal(updatedList)
            Log.d(TAG, "updateItemQuantity: New Cart Summary - items=${summary.totalItems}, price=${summary.totalPrice}")
            currentState.copy(menuItems = updatedList, cartSummary = summary)
        }
    }

    // ADDED: Clears all item quantities back to 0 completely and resets cart calculations safely
    fun clearCart() {
        Log.d(TAG, "clearCart: Resetting all menu item counts to 0")
        _uiState.update { currentState ->
            val resetList = currentState.menuItems.map { item ->
                item.copy(currentQuantity = 0)
            }
            originalFoodList = resetList
            val blankSummary = CartSummaryState(totalItems = 0, totalPrice = 0)
            currentState.copy(menuItems = resetList, cartSummary = blankSummary)
        }
    }

    fun selectCategory(categoryId: String) {
        Log.d(TAG, "selectCategory: categoryId=$categoryId")
        _uiState.update { currentState ->
            val updatedCategories = currentState.categories.map {
                it.copy(isSelected = it.id == categoryId)
            }
            currentState.copy(categories = updatedCategories)
        }
    }

    private fun calculateCartTotalsInternal(items: List<FoodItemData>): CartSummaryState {
        val activeSelections = items.filter { it.currentQuantity > 0 }
        val count = activeSelections.sumOf { it.currentQuantity }
        val total = activeSelections.sumOf { it.price * it.currentQuantity }
        return CartSummaryState(count, total)
    }
}