package com.example.masterdashboard.staff_dash.billing_screens.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.repo.CashierBillingRepository
import com.example.masterdashboard.staff_dash.billing_screens.uiState.CashierBillingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CashierBillingViewModel(
    private val repository: CashierBillingRepository = CashierBillingRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "CashierBillingVM"
    }

    private val _uiState = MutableStateFlow<CashierBillingUiState>(CashierBillingUiState.Loading)
    val uiState: StateFlow<CashierBillingUiState> = _uiState.asStateFlow()

    private var rawOrdersList: List<CashierBillingOrderModel> = emptyList()
    private var currentFilter: String = "All"
    private var currentSearchQuery: String = ""

    fun startListeningOrders(managerId: String) {
        viewModelScope.launch {
            _uiState.value = CashierBillingUiState.Loading
            repository.streamBillingOrders(managerId)
                .catch { e ->
                    Log.e(TAG, "Error in orders flow stream", e)
                    _uiState.value = CashierBillingUiState.Error(e.message ?: "Failed to load orders")
                }
                .collect { orders ->
                    rawOrdersList = orders
                    applyFilters()
                }
        }
    }

    fun setFilter(filterName: String) {
        currentFilter = filterName
        applyFilters()
    }

    fun searchOrders(query: String) {
        currentSearchQuery = query.trim()
        applyFilters()
    }

    private fun applyFilters() {
        // 1. Initial Data Source: rawOrdersList contains everything from this manager's path
        var filtered = rawOrdersList

        // 2. Filter by Chip selection
        if (currentFilter != "All") {
            filtered = when (currentFilter) {
                "Pending Bill" -> {
                    // Show orders that are NOT yet PAID
                    filtered.filter { 
                        val status = it.orderStatus.uppercase()
                        status != "PAID" && status != "COMPLETED"
                    }
                }
                "Take Away" -> {
                    // Specifically show Counter orders that are PAID but not yet Handed Over
                    filtered.filter { 
                        val status = it.orderStatus.uppercase()
                        val type = it.orderType.uppercase()
                        val isCounter = type == "TAKE_AWAY" || type == "DELIVERY"
                        isCounter && status == "PAID"
                    }
                }
                "Paid Bills" -> {
                    // Show PAID table bills and COMPLETED counter bills
                    filtered.filter { 
                        val status = it.orderStatus.uppercase()
                        val type = it.orderType.uppercase()
                        val isDineIn = type == "DINE_IN" || type.isEmpty()
                        (isDineIn && status == "PAID") || status == "COMPLETED"
                    }
                }
                else -> filtered
            }
        } else {
            // "All" filter: Show everything currently in progress or recently settled.
            // We only hide 'COMPLETED' (Handed over) from the 'All' view to keep it from getting cluttered.
            filtered = filtered.filter {
                it.orderStatus.uppercase() != "COMPLETED"
            }
        }

        // 3. Sort logic: Prioritize "BILLING" requested orders at the top, then newest timestamp
        filtered = filtered.sortedWith(compareByDescending<CashierBillingOrderModel> { 
            it.orderStatus.uppercase() == "BILLING" 
        }.thenByDescending { it.timestamp })

        // 4. Filter by Search Query (Table Name or Order ID)
        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.tableName.contains(currentSearchQuery, ignoreCase = true) ||
                        it.orderId.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        _uiState.value = CashierBillingUiState.Success(
            orders = filtered,
            selectedFilter = currentFilter
        )
    }

    /**
     * Marks a takeaway order as handed over.
     */
    fun confirmPickup(order: CashierBillingOrderModel) {
        viewModelScope.launch {
            repository.confirmTakeawayPickup(order).collect { resource ->
                // The repository listener will automatically refresh the list
                // we don't need to do much here unless we want to show a toast
            }
        }
    }

    class Factory(
        private val repository: CashierBillingRepository = CashierBillingRepository()
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CashierBillingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CashierBillingViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}