package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.CartSummaryState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderDataModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderItemModel
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.OrderUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
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

    private val _orderUploadStatus = MutableStateFlow<ResourceUiState<Boolean>?>(null)
    val orderUploadStatus: StateFlow<ResourceUiState<Boolean>?> = _orderUploadStatus.asStateFlow()

    var currentTableId: String? = null
        private set

    // Flag to prevent clearing data when returning from the Cart screen
    var isViewingCart: Boolean = false

    var originalFoodList: List<FoodItemData> = emptyList()
        private set

    // Persistent storage for the last generated Order ID to bridge navigation
    var lastOrderId: String? = null
        private set

    init {
        Log.d(TAG, "🏗️ [VIEWMODEL] OrderTakingViewModel instance successfully initialized.")
    }

    fun startOrderSession(tableId: String, status: String) {
        // If we are returning from the Cart, we do nothing and preserve the current state
        if (isViewingCart) {
            Log.d(TAG, "🏗️ [VIEWMODEL] Returning from Cart. Preserving state for Table: $tableId")
            isViewingCart = false
            return
        }

        // If it's a new entry from the Tables list, we refresh the session
        Log.i(TAG, "🏗️ [VIEWMODEL] Starting NEW session from Tables List for Table: $tableId [Status: $status]")
        currentTableId = tableId

        if (status.uppercase() == "FREE") {
            Log.d(TAG, "🏗️ [VIEWMODEL] Table is FREE. Clearing local cart quantities.")
            clearCart()
        } else {
            Log.d(TAG, "🏗️ [VIEWMODEL] Table is $status. Loading existing order context.")
            // For now, we clear the cart to ensure no leftover data from other tables, 
            // but in a real app, you'd call a repository method here to fetch the active order.
            clearCart()
        }
    }

    fun loadMenuData(managerId: String?) {
        // PREVENT REDUNDANT FETCH: If we already have data in the persistent originalFoodList,
        // we skip the fresh fetch to preserve existing cart quantities and prevent UI flickering.
        if (originalFoodList.isNotEmpty() && _uiState.value.categories.isNotEmpty()) {
            Log.d(TAG, "🏗️ [VIEWMODEL] Menu data already present in session cache. Skipping redundant fetch.")
            return
        }

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
                        // Maintain the currently selected category chip if categories update
                        val updatedCategories = dynamicCategories.map { incoming ->
                            val existingMatch = state.categories.find { it.id == incoming.id }
                            if (existingMatch != null) incoming.copy(isSelected = existingMatch.isSelected) else incoming
                        }
                        state.copy(categories = updatedCategories, isLoading = false)
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
                    _uiState.update { state ->
                        // PERSISTENCE LOGIC: 
                        // The repository might emit partial lists as it loads categories one by one.
                        // We merge the incoming items with our existing originalFoodList to ensure
                        // that items from other categories (and their quantities) are never wiped out.
                        
                        val quantityMap = originalFoodList.associateBy({ it.id }, { it.currentQuantity })
                        
                        val updatedList = dynamicFoodItems.map { newItem ->
                            newItem.copy(currentQuantity = quantityMap[newItem.id] ?: 0)
                        }.toMutableList()
                        
                        // Include items already in our master list that weren't part of this specific update
                        // (e.g. items from categories that are still loading or didn't change)
                        val incomingIds = dynamicFoodItems.map { it.id }.toSet()
                        val preservedItems = originalFoodList.filter { it.id !in incomingIds }
                        updatedList.addAll(preservedItems)

                        originalFoodList = updatedList
                        val summary = calculateCartTotalsInternal(updatedList)

                        // Respect the currently selected category view constraints
                        val selectedCategoryId = state.categories.find { it.isSelected }?.id ?: "ALL_ITEMS"
                        val filteredMenuItems = if (selectedCategoryId == "ALL_ITEMS") {
                            updatedList
                        } else {
                            updatedList.filter { it.categoryId == selectedCategoryId }
                        }

                        state.copy(
                            menuItems = filteredMenuItems,
                            cartSummary = summary,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Converts the current active quantities inside originalFoodList into a Firestore KOT ticket payload.
     */
    fun submitActiveOrderToKitchen(
        managerId: String?,
        floorId: String?,
        tableId: String?,
        specialNotes: String
    ) {
        val activeCartItems = originalFoodList.filter { it.currentQuantity > 0 }
        if (activeCartItems.isEmpty()) {
            Log.w(TAG, "🏗️ [VIEWMODEL] submitActiveOrderToKitchen: Cart is empty. Aborting submit.")
            return
        }

        // Map data items into a standard database dictionary schema format
        val itemsPayload = activeCartItems.map { item ->
            OrderItemModel(
                itemId = item.id,
                itemName = item.name,
                price = item.price,
                quantity = item.currentQuantity,
                rowTotal = (item.price * item.currentQuantity)
            )
        }

        // GENERATE ORDER ID: Storing it now to bridge the Success Fragment and the DB records
        val formattedOrderId = "#ORD-${(1000..9999).random()}"
        lastOrderId = formattedOrderId

        val subtotalValue = uiState.value.cartSummary.totalPrice.toDouble()
        val orderData = OrderDataModel(
            orderId = formattedOrderId,
            items = itemsPayload,
            specialNotes = specialNotes,
            subtotal = subtotalValue,
            gst = subtotalValue * 0.05,
            grandTotal = subtotalValue * 1.05,
            orderStatus = "PENDING", // Read by the kitchen display monitors
            timestamp = com.google.firebase.Timestamp.now()
        )

        Log.d(TAG, "🏗️ [VIEWMODEL] Dispatching KOT payload to repository for Table: $tableId")
        viewModelScope.launch {
            repository.sendOrderToFirebaseKitchen(managerId, floorId, tableId, orderData)
                .catch { exception ->
                    Log.e(TAG, "🏗️ [VIEWMODEL] Error uploading order to kitchen", exception)
                    _orderUploadStatus.value = com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState.Error(exception.message ?: "Unknown upload breakdown")
                }
                .collect { status ->
                    _orderUploadStatus.value = status
                }
        }
    }

    /**
     * Resets the upload transaction state tracking context back to clean default.
     */
    fun resetUploadStatus() {
        _orderUploadStatus.value = null
    }

    fun updateItemQuantity(foodId: String, increase: Boolean) {
        Log.d(TAG, "🏗️ [VIEWMODEL] updateItemQuantity execution -> ID: $foodId, Action Increase: $increase")
        _uiState.update { currentState ->
            val updatedOriginal = originalFoodList.map { item ->
                if (item.id == foodId) {
                    val newQty = if (increase) item.currentQuantity + 1 else maxOf(0, item.currentQuantity - 1)
                    item.copy(currentQuantity = newQty)
                } else item
            }

            originalFoodList = updatedOriginal

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

            // Always filter out of originalFoodList (the master list maintaining all active cart states)
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