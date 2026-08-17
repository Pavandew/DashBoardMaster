package com.example.masterdashboard.staff_dash.billing_screens.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.billing_screens.model.CashierBillingOrderModel
import com.example.masterdashboard.staff_dash.billing_screens.repo.CashierBillingRepository
import com.example.masterdashboard.staff_dash.billing_screens.uiState.CashierBillingUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
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
    private var isListening = false

    init {
        applyFilters() // Emit initial state with zero counts
    }

    fun startListeningOrders(managerId: String) {
        if (managerId.isEmpty()) return
        
        // Prevent restart if already listening
        if (isListening) {
            Log.d(TAG, "Already listening to billing stream. Skipping restart.")
            return
        }

        viewModelScope.launch {
            isListening = true
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
        // 1. Initial Data Source
        val relevantOrders = rawOrdersList.filter { order ->
            val status = order.orderStatus.uppercase()
            val type = order.orderType.uppercase()

            if (type == "TAKE_AWAY" || type == "DELIVERY") {
                status != "COMPLETED"
            } else {
                // Show SERVED (eating/done) and BILLING (ready to pay)
                status == "SERVED" || status == "BILLING" || status == "PAID" || status == "COMPLETED"
            }
        }

        // Calculate counts for chips
        val countAll = relevantOrders.count { it.orderStatus.uppercase() != "COMPLETED" }
        val countPending = relevantOrders.count { 
            val s = it.orderStatus.uppercase()
            val t = it.orderType.uppercase()
            if (t == "TAKE_AWAY" || t == "DELIVERY") s != "PAID" && s != "COMPLETED"
            else s == "SERVED" || s == "BILLING"
        }
        val countTakeAway = relevantOrders.count { 
            (it.orderType == "TAKE_AWAY" || it.orderType == "DELIVERY") && it.orderStatus.uppercase() == "PAID"
        }
        val countPaid = relevantOrders.count { 
            it.orderStatus.uppercase() == "PAID" || it.orderStatus.uppercase() == "COMPLETED"
        }

        // Ensure these are ALWAYS created even if relevantOrders is empty
        val computedFilters = listOf(
            TableFilterData("1", "All ($countAll)", currentFilter == "All"),
            TableFilterData("2", "Pending Bill ($countPending)", currentFilter == "Pending Bill"),
            TableFilterData("3", "Take Away ($countTakeAway)", currentFilter == "Take Away"),
            TableFilterData("4", "Paid Bills ($countPaid)", currentFilter == "Paid Bills")
        )

        var filtered = relevantOrders
        if (currentFilter != "All") {
            filtered = when (currentFilter) {
                "Pending Bill" -> filtered.filter { 
                    val s = it.orderStatus.uppercase()
                    if (it.orderType == "TAKE_AWAY" || it.orderType == "DELIVERY") s != "PAID" && s != "COMPLETED"
                    else s == "SERVED" || s == "BILLING"
                }
                "Take Away" -> filtered.filter { it.orderStatus.uppercase() == "PAID" && (it.orderType == "TAKE_AWAY" || it.orderType == "DELIVERY") }
                "Paid Bills" -> filtered.filter { it.orderStatus.uppercase() == "PAID" || it.orderStatus.uppercase() == "COMPLETED" }
                else -> filtered
            }
        } else {
            filtered = filtered.filter { it.orderStatus.uppercase() != "COMPLETED" }
        }

        filtered = filtered.sortedWith(compareByDescending<CashierBillingOrderModel> { it.orderStatus.uppercase() == "BILLING" }.thenByDescending { it.timestamp })

        if (currentSearchQuery.isNotEmpty()) {
            filtered = filtered.filter { it.tableName.contains(currentSearchQuery, true) || it.orderId.contains(currentSearchQuery, true) }
        }

        // Send Success state immediately so chips show up even with 0 orders
        _uiState.value = CashierBillingUiState.Success(orders = filtered, selectedFilter = currentFilter, filters = computedFilters)
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