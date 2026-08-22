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
        // INITIAL STATE: Provide a default "All" category so the UI isn't empty on first run
        val initialCategories = listOf(MenuCategoryData("ALL_ITEMS", "All", true))
        _uiState.update { it.copy(dietFilters = filters, categories = initialCategories) }
    }

    /**
     * Connects to Firestore listeners for food items.
     * Categories are now synced from the Session ViewModel via [syncCategories],
     * but we also maintain a local listener for first-time robustness.
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
        
        if (initialCategories.isNotEmpty()) {
            Log.d(TAG, "Load: Syncing provided initial categories.")
            syncCategories(initialCategories)
        }

        // Skip reload if items are already fully cached
        if (firestoreFoodList.isNotEmpty()) {
            Log.d(TAG, "Load: Items already cached. Refreshing UI.")
            buildDisplayList()
        }

        Log.i(TAG, "Load: Starting real-time Firestore catalog fetch for $managerId")
        _uiState.update { it.copy(isLoading = true) }

        // 1. Listen for Categories (Self-sufficient backup)
        viewModelScope.launch {
            repository.getMenuCategories(managerId)
                .catch { Log.e(TAG, "Load: Category fetch failed", it) }
                .collect { syncCategories(it) }
        }

        // 2. Listen for Food Items
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
            // Ensure "All" is at the start and rest are alphabetical
            val sorted = if (newCategories.size > 1) {
                val allChip = newCategories.find { it.id == "ALL_ITEMS" } ?: MenuCategoryData("ALL_ITEMS", "All", true)
                val others = newCategories.filter { it.id != "ALL_ITEMS" }.sortedBy { it.name.lowercase() }
                listOf(allChip) + others
            } else {
                newCategories
            }

            val updated = sorted.map { incoming ->
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
            val currentHighlight = state.categories.find { it.isSelected }?.id
            if (currentHighlight != categoryId) {
                val updated = state.categories.map { it.copy(isSelected = it.id == categoryId) }
                state.copy(categories = updated, activeFilterId = categoryId)
            } else state
        }
    }

    /**
     * CORE LOGIC: Filters the raw Firestore list and prepares it for the RecyclerView.
     * UPDATED: Now always builds the full grouped list to allow navigation-style scrolling.
     */
    private fun buildDisplayList() {
        val state = _uiState.value
        
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

        // 1. Apply Diet Filtering (Veg/Non-Veg)
        val dietFiltered = when (state.activeDietFilter) {
            "VEG" -> listWithQty.filter { it.isVeg }
            "NON-VEG" -> listWithQty.filter { !it.isVeg }
            else -> listWithQty
        }

        // 2. Navigation Mode: Always construct the full UI List with headers
        val displayItems = mutableListOf<MenuItemType>()
        
        // Group by category
        val grouped = dietFiltered.groupBy { it.categoryId }
        
        // Use the ordered categories to build the flat list for the Adapter
        state.categories.filter { it.id != "ALL_ITEMS" }.forEach { cat ->
            val items = grouped[cat.id]
            if (!items.isNullOrEmpty()) {
                displayItems.add(MenuItemType.Header(cat.id, cat.name))
                items.forEach { displayItems.add(MenuItemType.Food(it)) }
            }
        }

        Log.d(TAG, "Build: UI List ready with ${displayItems.size} items (Navigation Mode).")
        _uiState.update { it.copy(menuItems = dietFiltered, displayItems = displayItems, isLoading = false) }
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
