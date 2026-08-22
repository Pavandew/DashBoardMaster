package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.notifications.AppNotificationHelper
import com.example.masterdashboard.staff_dash.kitchen_screens.model.InventoryItem
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenInventoryRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KitchenInventoryViewModel(
    private val repository: KitchenInventoryRepository = KitchenInventoryRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "KitchenInventoryVM"
    }

    private val _rawItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    private val _selectedFilter = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Exposes unique categories present in the current inventory merged with common defaults.
     */
    val categories: StateFlow<List<String>> = _rawItems.map { items ->
        val defaultCategories = listOf("Vegetables", "Spices", "Dairy", "Grains", "Meat", "Oil & Fats", "Fruits", "Bakery", "Beverages", "Other")
        (defaultCategories + items.map { it.inventoryCategory }).filter { it.isNotEmpty() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Vegetables", "Spices", "Dairy", "Grains", "Meat", "Oil & Fats", "Other"))

    /**
     * Combined pipeline for filtering and searching inventory items.
     */
    val inventoryState: StateFlow<Pair<List<InventoryItem>, List<TableFilterData>>> = combine(
        _rawItems, _selectedFilter, _searchQuery
    ) { items, filter, query ->
        
        // 1. Calculate Filter Counts
        val allCount = items.size
        val inStockCount = items.count { it.getStockStatus() == "In Stock" }
        val lowStockCount = items.count { it.getStockStatus() == "Low Stock" }
        val outOfStockCount = items.count { it.getStockStatus() == "Out of Stock" }

        val filterChips = listOf(
            TableFilterData("1", "All ($allCount)", filter == "All"),
            TableFilterData("2", "In Stock ($inStockCount)", filter == "In Stock"),
            TableFilterData("3", "Low Stock ($lowStockCount)", filter == "Low Stock"),
            TableFilterData("4", "Out of Stock ($outOfStockCount)", filter == "Out of Stock")
        )

        // 2. Apply Stock Filter
        var filteredList = if (filter == "All") {
            items
        } else {
            items.filter { it.getStockStatus() == filter }
        }

        // 3. Apply Search Query
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.itemName.contains(query, ignoreCase = true) ||
                it.inventoryCategory.contains(query, ignoreCase = true)
            }
        }

        Pair(filteredList, filterChips)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), emptyList()))

    fun fetchInventory(restaurantId: String) {
        if (restaurantId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            repository.getInventoryItems(restaurantId).collectLatest { items ->
                _rawItems.value = items
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addItem(restaurantId: String, item: InventoryItem, staffName: String) {
        viewModelScope.launch {
            repository.addInventoryItem(restaurantId, item)
            
            // Notify Manager/Owner about the new item added
            val status = item.getStockStatus()
            val notifyType = if (status == "In Stock") "Added" else status

            AppNotificationHelper.notifyInventoryUpdate(
                managerId = restaurantId,
                staffName = staffName,
                itemName = item.itemName,
                quantity = item.itemQuantity,
                unit = item.itemUnit,
                type = notifyType
            )
        }
    }

    fun updateItem(restaurantId: String, item: InventoryItem, staffName: String) {
        viewModelScope.launch {
            repository.updateInventoryItem(restaurantId, item)
            
            // Notify Manager/Owner about the update
            val status = item.getStockStatus()
            val notifyType = if (status == "In Stock") "Updated" else status

            AppNotificationHelper.notifyInventoryUpdate(
                managerId = restaurantId,
                staffName = staffName,
                itemName = item.itemName,
                quantity = item.itemQuantity,
                unit = item.itemUnit,
                type = notifyType
            )
        }
    }

    fun deleteItem(restaurantId: String, inventoryId: String) {
        viewModelScope.launch {
            repository.deleteInventoryItem(restaurantId, inventoryId)
        }
    }
}
