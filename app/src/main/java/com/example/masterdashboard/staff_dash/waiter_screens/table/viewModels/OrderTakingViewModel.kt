package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.*
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.OrderTakingRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.notifications.AppNotificationHelper
import com.example.masterdashboard.utils.AppConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Shared ViewModel managing the active order session and cart contents.
 * Scoped to Activity to persist data across ordering fragments.
 */
class OrderTakingViewModel(private val repository: OrderTakingRepository) : ViewModel() {

    companion object {
        private const val TAG = "OrderTakingVM"
    }

    /**
     * Factory to instantiate the ViewModel with required repository.
     */
    class OrderViewModelFactory(private val repository: OrderTakingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when {
                modelClass.isAssignableFrom(OrderTakingViewModel::class.java) -> {
                    Log.d(TAG, "Factory: Creating OrderTakingViewModel")
                    OrderTakingViewModel(repository) as T
                }
                modelClass.isAssignableFrom(OrderMenuViewModel::class.java) -> {
                    Log.d(TAG, "Factory: Creating OrderMenuViewModel")
                    OrderMenuViewModel(repository) as T
                }
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    // --- State Observables ---

    private val _cartSummary = MutableStateFlow(CartSummaryState(0, 0))
    /**
     * Observable summary of the cart (total items, total price).
     */
    val cartSummary: StateFlow<CartSummaryState> = _cartSummary.asStateFlow()

    private val _orderUploadStatus = MutableStateFlow<ResourceUiState<String>?>(null)
    /**
     * Tracks the status of the order submission to Firestore.
     */
    val orderUploadStatus: StateFlow<ResourceUiState<String>?> = _orderUploadStatus.asStateFlow()

    private val _originalFoodList = MutableStateFlow<List<FoodItemData>>(emptyList())
    /**
     * The master list of menu items combined with their local quantities (acting as the cart source).
     */
    val originalFoodList: StateFlow<List<FoodItemData>> = _originalFoodList.asStateFlow()

    private val _categories = MutableStateFlow<List<MenuCategoryData>>(emptyList())
    /**
     * Observable list of menu categories for the current restaurant.
     */
    val categories: StateFlow<List<MenuCategoryData>> = _categories.asStateFlow()

    // --- Session Meta Data ---
    var currentTableId: String? = null
    var currentFloorId: String? = null
    var currentTableName: String = ""
    var existingOrderDocId: String? = null
    var existingOrderId: String? = null
    var isViewingCart: Boolean = false
    var lastOrderId: String? = null
    var customerName: String = ""
    var customerMobile: String = ""
    var orderType: String = "DINE_IN"
    var selectedPaymentMethod: String = ""

    // --- Data Loading ---

    /**
     * Loads categories and items from Firestore. 
     * Since this ViewModel is activity-scoped, this data stays "warm" across fragments.
     */
    fun loadCatalog(managerId: String?) {
        if (managerId.isNullOrEmpty()) return
        
        // If already loading or loaded, don't restart unless necessary
        if (_categories.value.isNotEmpty() && _originalFoodList.value.isNotEmpty()) {
            Log.d(TAG, "Catalog: Already loaded and active.")
            return
        }

        Log.i(TAG, "Catalog: Fetching menu from Firestore for manager: $managerId")
        
        // 1. Fetch Categories
        viewModelScope.launch {
            repository.getMenuCategories(managerId).collect { 
                Log.d(TAG, "Catalog: Received ${it.size} categories.")
                _categories.value = it 
            }
        }

        // 2. Fetch Food Items
        viewModelScope.launch {
            repository.getFoodMenu(managerId).collect { items ->
                Log.d(TAG, "Catalog: Received ${items.size} items.")
                syncMenuCatalog(items)
            }
        }
    }

    // --- Session Management ---

    /**
     * Prepares the shared session for a specific table.
     */
    fun startOrderSession(tableId: String, floorId: String, tableName: String, status: String, orderDocId: String? = null, orderId: String? = null) {
        Log.i(TAG, "Session: Starting for Table $tableName ($tableId). Status: $status, DocId: $orderDocId")
        
        // Prevent clearing if we are just returning to the same active session
        if (isViewingCart && currentTableId == tableId && existingOrderDocId == orderDocId) {
            Log.d(TAG, "Session: Returning to active session. State preserved.")
            isViewingCart = false
            return
        }

        currentTableId = tableId
        currentFloorId = floorId
        currentTableName = tableName
        existingOrderDocId = orderDocId
        existingOrderId = orderId
        isViewingCart = false

        // If table is free, clear any leftovers from previous Activity runs
        if (status.uppercase() == "FREE" || orderDocId.isNullOrEmpty()) {
            Log.d(TAG, "Session: Fresh table. Clearing local quantities.")
            clearCart()
        }
    }

    /**
     * Attempts to find an existing active order for the current table in Firestore.
     */
    fun findAndResumeOrderSession(managerId: String, floorId: String, tableId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Session: Searching Firestore for existing order on $tableId")
            val result = repository.getActiveOrderForTable(managerId, floorId, tableId)
            if (result != null) {
                val (docId, order) = result
                Log.i(TAG, "Session: Found existing order ${order.orderId}. Resuming...")
                existingOrderDocId = docId
                existingOrderId = if (order.orderId.isBlank() || order.orderId == docId) null else order.orderId
                resumeOrderSession(managerId, floorId, tableId, docId)
            }
        }
    }

    /**
     * Fetches details of an existing order and populates the local cart quantities.
     */
    fun resumeOrderSession(managerId: String, floorId: String, tableId: String, orderDocId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Session: Resuming order from DocId: $orderDocId")
            // Wait for menu items to be loaded
            var attempts = 0
            while (_originalFoodList.value.isEmpty() && attempts < 50) {
                delay(100)
                attempts++
            }

            val existingOrder = repository.getExistingOrder(managerId, floorId, tableId, orderDocId)
            if (existingOrder != null) {
                Log.i(TAG, "Session: Merging Firestore order data into local state.")
                customerName = existingOrder.customerName
                customerMobile = existingOrder.customerMobile
                orderType = existingOrder.orderType
                currentTableName = existingOrder.tableName.ifEmpty { currentTableName }
                
                val existingItemsMap = existingOrder.items.associateBy { it.itemId }
                _originalFoodList.value = _originalFoodList.value.map { item ->
                    val existing = existingItemsMap[item.id]
                    if (existing != null) {
                        // Sync quantities, status, variant AND the actual price from the existing order
                        item.copy(
                            currentQuantity = existing.quantity, 
                            previousQuantity = existing.quantity, 
                            readyQuantity = existing.readyQuantity,
                            itemStatus = existing.itemStatus, 
                            variantName = existing.variantName,
                            price = existing.price
                        )
                    } else {
                        item.copy(currentQuantity = 0, previousQuantity = 0, itemStatus = AppConstants.STATUS_PENDING)
                    }
                }
                updateTotals()
            } else {
                Log.e(TAG, "Session: Failed to fetch existing order document.")
            }
        }
    }

    // --- Cart Actions ---

    /**
     * Syncs the session's master list with the latest Firestore menu catalog.
     * Crucial: This preserves existing local quantities while adding new items from newly loaded categories.
     */
    fun syncMenuCatalog(list: List<FoodItemData>) {
        if (list.isEmpty()) return
        
        Log.v(TAG, "Cart: Syncing catalog. New list size: ${list.size}")
        
        if (_originalFoodList.value.isEmpty()) {
            Log.d(TAG, "Cart: Initializing master list with ${list.size} items.")
            _originalFoodList.value = list
        } else {
            // Map new catalog items while injecting our current local state (quantities, variants, and CUSTOM prices)
            val cartMap = _originalFoodList.value.associateBy({ it.id }, { it })
            val syncedList = list.map { newItem ->
                val local = cartMap[newItem.id]
                if (local != null) {
                    // Preserve local state if item was already modified in cart
                    newItem.copy(
                        currentQuantity = local.currentQuantity, 
                        previousQuantity = local.previousQuantity,
                        readyQuantity = local.readyQuantity,
                        itemStatus = local.itemStatus,
                        variantName = local.variantName,
                        price = local.price // CRITICAL: Preserve the price selected by user for this session
                    )
                } else newItem
            }
            
            // Safety Check: Also preserve items that might be in cart but somehow missing from the passed list
            val incomingIds = list.map { it.id }.toSet()
            val missingFromIncoming = _originalFoodList.value.filter { it.id !in incomingIds }
            
            _originalFoodList.value = syncedList + missingFromIncoming
        }
        updateTotals()
    }

    /**
     * Increments or decrements an item's quantity.
     */
    fun updateItemQuantity(foodId: String, increase: Boolean) {
        Log.v(TAG, "Cart: Updating quantity for $foodId. Increase: $increase")
        _originalFoodList.value = _originalFoodList.value.map { item ->
            if (item.id == foodId) {
                val newQty = if (increase) item.currentQuantity + 1 else maxOf(0, item.currentQuantity - 1)
                item.copy(currentQuantity = newQty)
            } else item
        }
        updateTotals()
    }

    /**
     * Updates an item with specific variants and addons from the customization UI.
     */
    fun setItemCustomization(foodId: String, quantity: Int, variant: String, addons: List<String>, variantPrice: Int? = null) {
        Log.d(TAG, "Cart: Applying customization for $foodId. Qty: $quantity, Variant: $variant")
        _originalFoodList.value = _originalFoodList.value.map { item ->
            if (item.id == foodId) {
                item.copy(
                    currentQuantity = quantity, 
                    variantName = variant, 
                    selectedAddons = addons, 
                    price = variantPrice ?: item.price
                )
            } else item
        }
        updateTotals()
    }

    /**
     * Resets all quantities to zero.
     */
    fun clearCart() {
        Log.i(TAG, "Cart: Resetting all item quantities to zero.")
        _originalFoodList.value = _originalFoodList.value.map { it.copy(currentQuantity = 0, previousQuantity = 0) }
        updateTotals()
    }

    /**
     * Recalculates total quantity and grand price for the cart summary.
     */
    private fun updateTotals() {
        val active = _originalFoodList.value.filter { it.currentQuantity > 0 }
        val totalQty = active.sumOf { it.currentQuantity }
        val totalPrice = active.sumOf { it.price * it.currentQuantity }
        Log.v(TAG, "Totals: Items: $totalQty, Value: ₹$totalPrice")
        _cartSummary.value = CartSummaryState(totalQty, totalPrice)
    }

    // --- Order Submission ---

    /**
     * Finalizes the cart and pushes it to Firestore.
     */
    fun submitActiveOrderToKitchen(managerId: String?, floorId: String?, tableId: String?, notes: String, initialStatus: String = "PENDING", waiterId: String = "") {
        val activeItems = _originalFoodList.value.filter { it.currentQuantity > 0 }
        if (activeItems.isEmpty()) {
            Log.w(TAG, "Submission: Blocked. Cart is empty.")
            return
        }

        Log.i(TAG, "Submission: Placing order for $currentTableName. Target Status: $initialStatus")

        val payload = activeItems.map { 
            OrderItemModel(
                it.id, 
                it.name, 
                it.variantName, 
                it.price, 
                it.currentQuantity, 
                it.price * it.currentQuantity, 
                it.previousQuantity, 
                it.readyQuantity,
                it.itemStatus
            ) 
        }
        
        val finalOrderId = existingOrderId ?: "#ORD-${(1000..9999).random()}"
        lastOrderId = finalOrderId

        val subtotal = _cartSummary.value.totalPrice.toDouble()
        val orderData = OrderDataModel(
            orderId = finalOrderId, 
            tableId = currentTableId ?: "",
            floorId = currentFloorId ?: "",
            tableName = currentTableName, 
            customerName = customerName, 
            customerMobile = customerMobile, 
            orderType = orderType, 
            items = payload, 
            specialNotes = notes, 
            subtotal = subtotal, 
            gst = subtotal * 0.05, 
            grandTotal = subtotal * 1.05, 
            orderStatus = initialStatus, 
            paymentMethod = selectedPaymentMethod, 
            restaurantId = managerId ?: "", 
            waiterId = waiterId
        )

        viewModelScope.launch {
            Log.d(TAG, "Submission: Uploading to Firebase path...")
            repository.sendOrderToFirebaseKitchen(managerId, floorId, tableId, orderData, existingOrderDocId)
                .catch { e -> 
                    Log.e(TAG, "Submission: Firestore error", e)
                    _orderUploadStatus.value = ResourceUiState.Error(e.message ?: "Firestore failure") 
                }
                .collect { status ->
                    // Map results for UI observation, preserving the Doc Path string on success
                    val uiStatus = when(status) {
                        is ResourceUiState.Success -> ResourceUiState.Success(status.data)
                        is ResourceUiState.Error -> ResourceUiState.Error(status.message)
                        is ResourceUiState.Loading -> ResourceUiState.Loading
                        else -> ResourceUiState.Idle
                    }
                    _orderUploadStatus.value = uiStatus as ResourceUiState<String>
                    
                    if (status is ResourceUiState.Success && managerId != null) {
                        Log.i(TAG, "Submission: Successful. Saved at: ${status.data}")
                        // Mark currently sent quantities as "previous" to avoid re-notifying chef for same count
                        _originalFoodList.value = _originalFoodList.value.map { it.copy(previousQuantity = it.currentQuantity) }
                        notifyChef(managerId, currentTableName, finalOrderId, waiterId, status.data)
                    }
                }
        }
    }

    /**
     * Triggers notifications and logs history for the kitchen staff.
     */
    private suspend fun notifyChef(managerId: String, tableName: String, orderId: String, waiterId: String, docPath: String) {
        Log.d(TAG, "Notification: Fetching chef tokens for restaurant: $managerId")
        val tokens = repository.getChefTokens(managerId)
        AppNotificationHelper.notifyKitchenOfNewOrder(tokens, tableName, orderId, managerId, waiterId, docPath)
    }

    // --- Helper Utilities ---
    fun setCustomerDetails(name: String, phone: String, type: String) { customerName = name; customerMobile = phone; orderType = type }
    fun setPaymentMethod(method: String) { selectedPaymentMethod = method }
    fun resetUploadStatus() { _orderUploadStatus.value = null }
}
