package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.*
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Specialized ViewModel for managing the Menu Catalog UI part of the screen.
 * Handles Firestore real-time fetching, diet/category filtering, and grouping for the RecyclerView.
 */
class OrderMenuViewModel(private val repository: OrderTakingRepository) : ViewModel() {

    private val TAG = "OrderMenuVM"

    // --- UI STATE ---
    private val _uiState = MutableStateFlow(OrderMenuUiState())
    /**
     * Observable state for the Menu UI (Categories, Items, Loading status).
     */
    val uiState: StateFlow<OrderMenuUiState> = _uiState.asStateFlow()

    // Internal cache of all food items loaded from Firestore
    private var firestoreFoodList: List<FoodItemData> = emptyList()
    
    // Internal cache of the latest cart snapshot to ensure UI is always in sync
    private var lastCartSnapshot: List<FoodItemData> = emptyList()

    init {
        Log.d(TAG, "Initialization: Setting up diet filters.")
        val filters = listOf(
            MenuCategoryData("ALL", "All", true),
            MenuCategoryData("VEG", "Veg", false),
            MenuCategoryData("NON-VEG", "Non-Veg", false)
        )
        _uiState.update { it.copy(dietFilters = filters) }
    }

    /**
     * Connects to Firestore listeners for food items.
     * Categories are now synced from the Session ViewModel via [syncCategories].
     */
    fun loadMenuData(
        managerId: String?, 
        currentCart: List<FoodItemData>, 
        initialCategories: List<MenuCategoryData> = emptyList(),
        onCatalogSynced: (List<FoodItemData>) -> Unit
    ) {
        if (managerId.isNullOrEmpty()) {
            Log.e(TAG, "Load: Aborted. Manager ID is null.")
            return
        }
        
        lastCartSnapshot = currentCart

        // FIX 1: If the session already has data, use it as initial cache
        if (currentCart.isNotEmpty() && firestoreFoodList.isEmpty()) {
            Log.d(TAG, "Load: Initializing internal cache from existing session cart.")
            firestoreFoodList = currentCart
        }
        
        if (initialCategories.isNotEmpty() && _uiState.value.categories.isEmpty()) {
            Log.d(TAG, "Load: Initializing categories from session cache.")
            _uiState.update { it.copy(categories = initialCategories) }
        }

        // Skip reload if items are already fully cached
        if (firestoreFoodList.isNotEmpty()) {
            Log.d(TAG, "Load: Items already cached. Refreshing UI.")
            buildDisplayList()
        }

        Log.i(TAG, "Load: Starting real-time Firestore item fetch for $managerId")
        _uiState.update { it.copy(isLoading = true) }

        // 1. Listen for Food Items
        viewModelScope.launch {
            repository.getFoodMenu(managerId)
                .catch { e -> 
                    Log.e(TAG, "Load: Items fetch failed", e)
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } 
                }
                .collect { items ->
                    Log.i(TAG, "Load: Received ${items.size} total items from Firestore.")
                    firestoreFoodList = items
                    // Push back to session VM to maintain quantities across fragments
                    onCatalogSynced(items)
                    buildDisplayList()
                }
        }
    }

    /**
     * Synchronizes categories from the persistent Session ViewModel.
     * This ensures the Menu UI reacts to category updates even on the first run.
     */
    fun syncCategories(newCategories: List<MenuCategoryData>) {
        if (newCategories.isEmpty()) return
        Log.d(TAG, "Sync: Received ${newCategories.size} categories from Session.")
        
        _uiState.update { state ->
            val updated = newCategories.map { incoming ->
                // Preserve local selection if it exists, otherwise use incoming selection
                val match = state.categories.find { it.id == incoming.id }
                if (match != null) incoming.copy(isSelected = match.isSelected) else incoming
            }
            state.copy(categories = updated)
        }
        buildDisplayList()
    }

    /**
     * Refreshes the display list whenever the cart quantities change.
     */
    fun refreshMenuWithCart(currentCart: List<FoodItemData>) {
        lastCartSnapshot = currentCart
        
        // FIX 2: Only trigger a rebuild if we already have the menu categories.
        // This prevents early "cartSummary" updates from setting isLoading = false before items arrive.
        if (_uiState.value.categories.isNotEmpty()) {
            buildDisplayList()
        }
    }

    /**
     * Handles Category Chip clicks.
     */
    fun selectCategory(categoryId: String, currentCart: List<FoodItemData>) {
        lastCartSnapshot = currentCart
        _uiState.update { state ->
            val updated = state.categories.map { it.copy(isSelected = it.id == categoryId) }
            state.copy(categories = updated, activeFilterId = categoryId)
        }
        buildDisplayList()
    }

    /**
     * Handles Diet (Veg/Non-Veg) filter clicks.
     */
    fun selectDietFilter(dietId: String, currentCart: List<FoodItemData>) {
        lastCartSnapshot = currentCart
        _uiState.update { state ->
            val updated = state.dietFilters.map { it.copy(isSelected = it.id == dietId) }
            state.copy(dietFilters = updated, activeDietFilter = dietId)
        }
        buildDisplayList()
    }

    /**
     * Synchronizes chip highlight when user scrolls through the list.
     */
    fun syncCategoryHighlight(categoryId: String) {
        _uiState.update { state ->
            if (state.activeFilterId != "ALL_ITEMS") return@update state
            
            val currentHighlight = state.categories.find { it.isSelected }?.id
            if (currentHighlight != categoryId) {
                val updated = state.categories.map { it.copy(isSelected = it.id == categoryId) }
                state.copy(categories = updated)
            } else state
        }
    }

    /**
     * CORE LOGIC: Filters the raw Firestore list and prepares it for the RecyclerView.
     */
    private fun buildDisplayList() {
        val state = _uiState.value
        
        // FIX 3: Do not finish loading if we don't even have categories yet.
        if (state.categories.isEmpty()) {
            Log.v(TAG, "Build: Postponed. Categories not yet loaded.")
            return
        }

        // Merge state from the latest cart snapshot (Quantities, Variants, and Prices)
        val cartMap = lastCartSnapshot.associateBy { it.id }
        val listWithQty = firestoreFoodList.map { item ->
            val cartItem = cartMap[item.id]
            if (cartItem != null) {
                item.copy(
                    currentQuantity = cartItem.currentQuantity, 
                    variantName = cartItem.variantName,
                    price = cartItem.price
                )
            } else {
                item.copy(currentQuantity = 0)
            }
        }

        // 1. Apply Diet Filtering
        val dietFiltered = when (state.activeDietFilter) {
            "VEG" -> listWithQty.filter { it.isVeg }
            "NON-VEG" -> listWithQty.filter { !it.isVeg }
            else -> listWithQty
        }

        // 2. Apply Category Filtering
        val filtered = if (state.activeFilterId == "ALL_ITEMS") dietFiltered else dietFiltered.filter { it.categoryId == state.activeFilterId }

        // 3. Construct UI List with headers
        val displayItems = mutableListOf<MenuItemType>()
        if (state.activeFilterId == "ALL_ITEMS") {
            // Group by category and add headers
            val grouped = dietFiltered.groupBy { it.categoryId }
            state.categories.filter { it.id != "ALL_ITEMS" }.forEach { cat ->
                val items = grouped[cat.id]
                if (!items.isNullOrEmpty()) {
                    displayItems.add(MenuItemType.Header(cat.id, cat.name))
                    items.forEach { displayItems.add(MenuItemType.Food(it)) }
                }
            }
        } else {
            // Single category view: Add one header and all matching items
            val name = state.categories.find { it.id == state.activeFilterId }?.name ?: "Items"
            displayItems.add(MenuItemType.Header(state.activeFilterId, name))
            filtered.forEach { displayItems.add(MenuItemType.Food(it)) }
        }

        Log.d(TAG, "Build: UI List ready with ${displayItems.size} items.")
        _uiState.update { it.copy(menuItems = filtered, displayItems = displayItems, isLoading = false) }
    }

    fun getRawFoodList() = firestoreFoodList
}

data class OrderMenuUiState(
    val isLoading: Boolean = false,
    val categories: List<MenuCategoryData> = emptyList(),
    val dietFilters: List<MenuCategoryData> = emptyList(),
    val activeFilterId: String = "ALL_ITEMS",
    val activeDietFilter: String = "ALL",
    val menuItems: List<FoodItemData> = emptyList(),
    val displayItems: List<MenuItemType> = emptyList(),
    val errorMessage: String? = null
)
