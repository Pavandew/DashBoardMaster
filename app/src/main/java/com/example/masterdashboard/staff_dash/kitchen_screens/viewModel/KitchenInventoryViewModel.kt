package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.kitchen_screens.model.InventoryItem
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenInventoryRepository
import com.example.masterdashboard.notifications.AppNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KitchenInventoryViewModel(
    private val repository: KitchenInventoryRepository = KitchenInventoryRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "KitchenInventoryVM"
    }

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Fetches the inventory list from Firestore for the given restaurant.
     */
    fun fetchInventory(restaurantId: String) {
        if (restaurantId.isEmpty()) {
            Log.e(TAG, "fetchInventory: Failed! restaurantId is empty.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            Log.d(TAG, "fetchInventory: Starting real-time listener for restaurant: $restaurantId")
            repository.getInventoryItems(restaurantId).collectLatest { items ->
                _inventoryItems.value = items
                _isLoading.value = false
                Log.d(TAG, "fetchInventory: Successfully fetched ${items.size} items.")
            }
        }
    }

    /**
     * Adds a new item to the kitchen inventory and triggers a manager notification.
     */
    fun addItem(restaurantId: String, item: InventoryItem, staffName: String) {
        if (restaurantId.isEmpty()) {
            Log.e(TAG, "addItem: Failed! restaurantId is empty.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "addItem: Adding item '${item.itemName}' for restaurant: $restaurantId")
                repository.addInventoryItem(restaurantId, item)
                
                Log.d(TAG, "addItem: Item added. Sending notification to manager.")
                AppNotificationHelper.notifyInventoryUpdate(
                    managerId = restaurantId,
                    staffName = staffName,
                    itemName = item.itemName,
                    quantity = item.itemQuantity,
                    unit = item.itemUnit,
                    type = "Added"
                )
            } catch (e: Exception) {
                Log.e(TAG, "addItem: Error adding item flow", e)
            }
        }
    }

    /**
     * Updates an existing item and notifies the manager.
     */
    fun updateItem(restaurantId: String, item: InventoryItem, staffName: String) {
        if (restaurantId.isEmpty()) {
            Log.e(TAG, "updateItem: Failed! restaurantId is empty.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "updateItem: Updating item '${item.itemName}' (ID: ${item.inventoryId})")
                repository.updateInventoryItem(restaurantId, item)
                
                Log.d(TAG, "updateItem: Item updated. Sending notification to manager.")
                AppNotificationHelper.notifyInventoryUpdate(
                    managerId = restaurantId,
                    staffName = staffName,
                    itemName = item.itemName,
                    quantity = item.itemQuantity,
                    unit = item.itemUnit,
                    type = "Updated"
                )
            } catch (e: Exception) {
                Log.e(TAG, "updateItem: Error updating item flow", e)
            }
        }
    }

    /**
     * Removes an item from the inventory.
     */
    fun deleteItem(restaurantId: String, inventoryId: String) {
        if (restaurantId.isEmpty() || inventoryId.isEmpty()) {
            Log.e(TAG, "deleteItem: Failed! Missing IDs. restaurantId: $restaurantId, inventoryId: $inventoryId")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "deleteItem: Deleting inventory item with ID: $inventoryId")
                repository.deleteInventoryItem(restaurantId, inventoryId)
                Log.d(TAG, "deleteItem: Item deleted successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "deleteItem: Error deleting item flow", e)
            }
        }
    }
}
