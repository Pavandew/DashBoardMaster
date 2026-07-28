package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.CartSummaryState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.FoodItemData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuCategoryData
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.MenuItemType
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

/**
 * Shared ViewModel managing the order creation and modification lifecycle.
 */
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

    var currentTableName: String = ""
        private set

    var existingOrderDocId: String? = null
        private set

    var existingOrderId: String? = null
        private set

    var isViewingCart: Boolean = false

    var originalFoodList: List<FoodItemData> = emptyList()
        private set

    var lastOrderId: String? = null
        private set

    var customerName: String = ""
    var customerPhone: String = ""
    var orderType: String = "NORMAL"

    init {
        Log.d(TAG, "🏗️ [VIEWMODEL] OrderTakingViewModel instance successfully initialized.")
        initializeDietFilters()
    }

    private fun initializeDietFilters() {
        val filters = listOf(
            MenuCategoryData("ALL", "All", true),
            MenuCategoryData("VEG", "Veg", false),
            MenuCategoryData("NON-VEG", "Non-Veg", false)
        )
        _uiState.update { it.copy(dietFilters = filters) }
    }

    fun setCustomerDetails(name: String, phone: String, type: String) {
        customerName = name
        customerPhone = phone
        orderType = type
    }

    fun startOrderSession(
        tableId: String,
        tableName: String,
        status: String,
        orderDocId: String? = null,
        orderId: String? = null
    ) {
        if (isViewingCart) {
            Log.d(TAG, "🏗️ [VIEWMODEL] Returning from Cart. Preserving state for Table: $tableId")
            isViewingCart = false
            return
        }

        Log.i(TAG, "🏗️ [VIEWMODEL] startOrderSession() -> Table: $tableId ($tableName), Status: $status, ExistingDoc: $orderDocId")
        currentTableId = tableId
        currentTableName = tableName
        existingOrderDocId = orderDocId
        existingOrderId = orderId

        if (status.uppercase() == "FREE") {
            Log.d(TAG, "🏗️ [VIEWMODEL] Table is FREE. Clearing local cart quantities.")
            clearCart()
        } else if (!orderDocId.isNullOrEmpty()) {
            Log.d(TAG, "🏗️ [VIEWMODEL] Table is $status with existing order. Awaiting data merge...")
        } else {
            clearCart()
        }
    }

    fun resumeOrderSession(
        managerId: String,
        floorId: String,
        tableId: String,
        orderDocId: String
    ) {
        viewModelScope.launch {
            Log.d(TAG, "🏗️ [VIEWMODEL] resumeOrderSession() triggered. Waiting for menu items to load...")
            var attempts = 0
            while (originalFoodList.isEmpty() && attempts < 50) {
                kotlinx.coroutines.delay(100)
                attempts++
            }

            if (originalFoodList.isEmpty()) {
                Log.e(TAG, "🏗️ [VIEWMODEL] resumeOrderSession failed: originalFoodList is still empty after delay.")
                return@launch
            }

            Log.d(TAG, "🏗️ [VIEWMODEL] Fetching existing order document from Firestore: $orderDocId")
            val existingOrder = repository.getExistingOrder(managerId, floorId, tableId, orderDocId)
            if (existingOrder != null) {
                Log.i(TAG, "🏗️ [VIEWMODEL] Existing order data received. Merging ${existingOrder.items.size} items and customer details.")
                
                customerName = existingOrder.customerName
                customerPhone = existingOrder.customerPhone
                orderType = existingOrder.orderType
                currentTableName = existingOrder.tableName.ifEmpty { currentTableName }
                
                val existingQtyMap = existingOrder.items.associate { it.itemId to it.quantity }
                
                _uiState.update { state ->
                    val updatedOriginal = originalFoodList.map { foodItem ->
                        val existingQty = existingQtyMap[foodItem.id] ?: 0
                        foodItem.copy(
                            currentQuantity = existingQty,
                            previousQuantity = existingQty
                        )
                    }
                    originalFoodList = updatedOriginal
                    val summary = calculateCartTotalsInternal(updatedOriginal)
                    updateDisplayList(state, updatedOriginal, state.activeFilterId, summary)
                }
            } else {
                Log.e(TAG, "🏗️ [VIEWMODEL] Failed to fetch existing order: Document not found or error occurred.")
            }
        }
    }

    fun loadMenuData(managerId: String?) {
        if (originalFoodList.isNotEmpty() && _uiState.value.categories.isNotEmpty()) {
            Log.d(TAG, "🏗️ [VIEWMODEL] Menu data already present in cache. Skipping redundant fetch.")
            return
        }

        if (managerId.isNullOrEmpty()) {
            Log.e(TAG, "🏗️ [VIEWMODEL] loadMenuData: Manager ID is null or empty.")
            _uiState.update { it.copy(errorMessage = "Session mismatch error", isLoading = false) }
            return
        }

        Log.i(TAG, "🏗️ [VIEWMODEL] loadMenuData() started for ID: $managerId")
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // 1. Fetch Menu Categories
        viewModelScope.launch {
            repository.getMenuCategories(managerId)
                .catch { e ->
                    Log.e(TAG, "🏗️ [VIEWMODEL] Category stream error", e)
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { dynamicCategories ->
                    Log.d(TAG, "🏗️ [VIEWMODEL] Received ${dynamicCategories.size} menu categories.")
                    _uiState.update { state ->
                        val updatedCategories = dynamicCategories.map { incoming ->
                            // Maintain existing visual selection
                            val existingMatch = state.categories.find { it.id == incoming.id }
                            if (existingMatch != null) incoming.copy(isSelected = existingMatch.isSelected) else incoming
                        }
                        
                        val summary = calculateCartTotalsInternal(originalFoodList)
                        
                        // CRITICAL: Return the new state with updated categories and grouped display items
                        updateDisplayList(state.copy(categories = updatedCategories), originalFoodList, state.activeFilterId, summary)
                    }
                }
        }

        // 2. Fetch Nested Menu Food Items
        viewModelScope.launch {
            repository.getFoodMenu(managerId)
                .catch { e ->
                    Log.e(TAG, "🏗️ [VIEWMODEL] Food menu stream error", e)
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { dynamicFoodItems ->
                    Log.d(TAG, "🏗️ [VIEWMODEL] Received menu update: ${dynamicFoodItems.size} items total.")
                    
                    val quantityMap = originalFoodList.associateBy({ it.id }, { it.currentQuantity })
                    val updatedList = dynamicFoodItems.map { newItem ->
                        newItem.copy(currentQuantity = quantityMap[newItem.id] ?: 0)
                    }.toMutableList()
                    
                    val incomingIds = dynamicFoodItems.map { it.id }.toSet()
                    val preservedItems = originalFoodList.filter { it.id !in incomingIds }
                    updatedList.addAll(preservedItems)

                    originalFoodList = updatedList
                    val summary = calculateCartTotalsInternal(updatedList)

                    _uiState.update { state ->
                        updateDisplayList(state, updatedList, state.activeFilterId, summary)
                    }
                }
        }
    }

    private fun updateDisplayList(
        state: OrderUiState,
        fullList: List<FoodItemData>,
        activeFilterId: String,
        summary: CartSummaryState
    ): OrderUiState {
        val activeDietFilter = state.activeDietFilter
        Log.d(TAG, "🏗️ [VIEWMODEL] updateDisplayList() -> Active Filter: $activeFilterId, Diet: $activeDietFilter")
        
        // 1. Apply Diet Filter first
        val dietFilteredList = when (activeDietFilter) {
            "VEG" -> fullList.filter { it.isVeg }
            "NON-VEG" -> fullList.filter { !it.isVeg }
            else -> fullList
        }

        // 2. Apply Category Filter
        val filteredMenuItems = if (activeFilterId == "ALL_ITEMS") {
            dietFilteredList
        } else {
            dietFilteredList.filter { it.categoryId == activeFilterId }
        }

        val displayItems = mutableListOf<MenuItemType>()
        if (activeFilterId == "ALL_ITEMS") {
            val grouped = dietFilteredList.groupBy { it.categoryId }
            val actualCategories = state.categories.filter { it.id != "ALL_ITEMS" }
            
            actualCategories.forEach { category ->
                val itemsInCategory = grouped[category.id]
                if (!itemsInCategory.isNullOrEmpty()) {
                    displayItems.add(MenuItemType.Header(category.id, category.name))
                    itemsInCategory.forEach { displayItems.add(MenuItemType.Food(it)) }
                }
            }
            
            if (displayItems.isEmpty() && dietFilteredList.isNotEmpty()) {
                Log.w(TAG, "🏗️ [VIEWMODEL] updateDisplayList: No categories matched food items yet. Fallback grouping.")
                grouped.forEach { (catId, items) ->
                    displayItems.add(MenuItemType.Header(catId, "Section"))
                    items.forEach { displayItems.add(MenuItemType.Food(it)) }
                }
            }
        } else {
            val categoryName = state.categories.find { it.id == activeFilterId }?.name ?: "Items"
            displayItems.add(MenuItemType.Header(activeFilterId, categoryName))
            filteredMenuItems.forEach { displayItems.add(MenuItemType.Food(it)) }
        }

        return state.copy(
            categories = state.categories,
            activeFilterId = activeFilterId,
            menuItems = filteredMenuItems,
            displayItems = displayItems,
            cartSummary = summary,
            isLoading = false
        )
    }

    fun selectDietFilter(dietId: String) {
        Log.d(TAG, "🏗️ [VIEWMODEL] selectDietFilter() clicked -> $dietId")
        _uiState.update { currentState ->
            val updatedDietFilters = currentState.dietFilters.map {
                it.copy(isSelected = it.id == dietId)
            }
            val summary = calculateCartTotalsInternal(originalFoodList)
            updateDisplayList(
                currentState.copy(dietFilters = updatedDietFilters, activeDietFilter = dietId),
                originalFoodList,
                currentState.activeFilterId,
                summary
            )
        }
    }

    fun submitActiveOrderToKitchen(
        managerId: String?,
        floorId: String?,
        tableId: String?,
        specialNotes: String
    ) {
        val activeCartItems = originalFoodList.filter { it.currentQuantity > 0 }
        if (activeCartItems.isEmpty()) {
            Log.w(TAG, "🏗️ [VIEWMODEL] submitActiveOrderToKitchen: Cart is empty.")
            return
        }

        Log.i(TAG, "🏗️ [VIEWMODEL] Submitting Order for Table: $tableId, Manager: $managerId")

        val itemsPayload = activeCartItems.map { item ->
            OrderItemModel(
                itemId = item.id,
                itemName = item.name,
                price = item.price,
                quantity = item.currentQuantity,
                rowTotal = (item.price * item.currentQuantity),
                orderedQuantity = item.currentQuantity
            )
        }

        val finalOrderId = existingOrderId ?: "#ORD-${(1000..9999).random()}"
        lastOrderId = finalOrderId

        val subtotalValue = uiState.value.cartSummary.totalPrice.toDouble()
        val orderData = OrderDataModel(
            orderId = finalOrderId,
            tableName = currentTableName,
            customerName = customerName,
            customerPhone = customerPhone,
            orderType = orderType,
            items = itemsPayload,
            specialNotes = specialNotes,
            subtotal = subtotalValue,
            gst = subtotalValue * 0.05,
            grandTotal = subtotalValue * 1.05,
            orderStatus = "PENDING",
            timestamp = com.google.firebase.Timestamp.now()
        )

        viewModelScope.launch {
            Log.d(TAG, "🏗️ [VIEWMODEL] Calling repository to save order: $finalOrderId")
            repository.sendOrderToFirebaseKitchen(managerId, floorId, tableId, orderData, existingOrderDocId)
                .catch { e ->
                    Log.e(TAG, "🏗️ [VIEWMODEL] Order submission failed", e)
                    _orderUploadStatus.value = ResourceUiState.Error(e.message ?: "Unknown upload error")
                }
                .collect { status ->
                    _orderUploadStatus.value = status
                }
        }
    }

    fun resetUploadStatus() {
        _orderUploadStatus.value = null
    }

    fun updateItemQuantity(foodId: String, increase: Boolean) {
        Log.d(TAG, "🏗️ [VIEWMODEL] updateItemQuantity() -> ID: $foodId, Increase: $increase")
        _uiState.update { currentState ->
            val updatedOriginal = originalFoodList.map { item ->
                if (item.id == foodId) {
                    val newQty = if (increase) item.currentQuantity + 1 else maxOf(0, item.currentQuantity - 1)
                    item.copy(currentQuantity = newQty)
                } else item
            }
            originalFoodList = updatedOriginal

            val summary = calculateCartTotalsInternal(updatedOriginal)
            updateDisplayList(currentState, updatedOriginal, currentState.activeFilterId, summary)
        }
    }

    /**
     * Sets an absolute quantity for a food item (used by customization screens).
     */
    fun setItemQuantity(foodId: String, quantity: Int) {
        Log.d(TAG, "🏗️ [VIEWMODEL] setItemQuantity() -> ID: $foodId, Qty: $quantity")
        _uiState.update { currentState ->
            val updatedOriginal = originalFoodList.map { item ->
                if (item.id == foodId) {
                    item.copy(currentQuantity = quantity)
                } else item
            }
            originalFoodList = updatedOriginal

            val summary = calculateCartTotalsInternal(updatedOriginal)
            updateDisplayList(currentState, updatedOriginal, currentState.activeFilterId, summary)
        }
    }

    fun clearCart() {
        Log.d(TAG, "🏗️ [VIEWMODEL] clearCart() called.")
        _uiState.update { currentState ->
            val resetOriginal = originalFoodList.map { it.copy(currentQuantity = 0) }
            originalFoodList = resetOriginal
            val summary = CartSummaryState(0, 0)
            updateDisplayList(currentState, resetOriginal, currentState.activeFilterId, summary)
        }
    }

    /**
     * Triggered when a user CLICKS a category chip. 
     * Updates both the active filter (data) and visual selection (UI).
     */
    fun selectCategory(categoryId: String) {
        Log.d(TAG, "🏗️ [VIEWMODEL] selectCategory() clicked -> $categoryId")
        _uiState.update { currentState ->
            val updatedCategories = currentState.categories.map {
                it.copy(isSelected = it.id == categoryId)
            }
            val summary = calculateCartTotalsInternal(originalFoodList)
            updateDisplayList(
                currentState.copy(categories = updatedCategories, activeFilterId = categoryId), 
                originalFoodList, 
                categoryId, 
                summary
            )
        }
    }

    /**
     * Triggered automatically during SCROLLING.
     * Updates ONLY the visual visual selection (chip highlight) without changing the filter.
     */
    fun syncCategorySelectionFromScroll(categoryId: String) {
        _uiState.update { currentState ->
            // ONLY sync chip highlights if we are currently in "ALL_ITEMS" mode
            if (currentState.activeFilterId != "ALL_ITEMS") return@update currentState

            val currentVisualSelected = currentState.categories.find { it.isSelected }?.id
            if (currentVisualSelected != categoryId) {
                Log.d(TAG, "🏗️ [VIEWMODEL] syncCategorySelectionFromScroll() -> Highlighting chip: $categoryId")
                val updatedCategories = currentState.categories.map {
                    it.copy(isSelected = it.id == categoryId)
                }
                // Return new state with updated categories but SAME activeFilterId and SAME displayItems
                currentState.copy(categories = updatedCategories)
            } else {
                currentState
            }
        }
    }

    private fun calculateCartTotalsInternal(items: List<FoodItemData>): CartSummaryState {
        val activeSelections = items.filter { it.currentQuantity > 0 }
        val count = activeSelections.sumOf { it.currentQuantity }
        val total = activeSelections.sumOf { it.price * it.currentQuantity }
        return CartSummaryState(count, total)
    }
}
