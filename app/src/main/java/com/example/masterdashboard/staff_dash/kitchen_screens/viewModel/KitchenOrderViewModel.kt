package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenOrderRepository
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KitchenOrderViewModel(private val repository: KitchenOrderRepository = KitchenOrderRepository()) : ViewModel() {

    private val _rawOrders = MutableStateFlow<List<KitchenOrderDetailData>>(emptyList())
    // Defaulting to "New" so KitchenOrderFragment shows fresh tickets by default
    private val _selectedStatusFilter = MutableStateFlow("New")
    private val _selectedTypeFilter = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _isWorkstationContext = MutableStateFlow(false)
    private val _error = MutableStateFlow<Throwable?>(null)
    private val _isLoading = MutableStateFlow(false)

    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    // Fuses data stream with Status, Type, and Search filters
    val uiState: StateFlow<KitchenOrderUiState> = combine(
        combine(_rawOrders, _selectedStatusFilter, _selectedTypeFilter, _isWorkstationContext) { orders, status, type, isWorkstation ->
            DataBundle(orders, status, type, isWorkstation)
        },
        _searchQuery,
        _error,
        _isLoading
    ) { bundle, query, error, loading ->
        val (rawList, statusFilter, typeFilter, isWorkstation) = bundle
        when {
            error != null -> KitchenOrderUiState.Error(error)
            loading && rawList.isEmpty() -> KitchenOrderUiState.Loading
            else -> {
                // Filter out orders that are finalized (Paid/Rejected)
                // We now allow "Completed" (Served) to be visible in the workstation for the chef's reference
                var filtered = rawList.filter {
                    val s = it.status.uppercase()
                    s != "PAID" && s != "REJECTED"
                }

                // 1. Status Filter (e.g., New, Preparing, Ready, Completed)
                if (statusFilter != "All") {
                    filtered = filtered.filter {
                        it.status.equals(statusFilter, ignoreCase = true)
                    }
                } else {
                    if (isWorkstation) {
                        // Workstation "All" view: Show Preparing + Ready + Completed
                        filtered = filtered.filter { it.status.uppercase() != "NEW" && it.status.uppercase() != "PENDING" }
                    } else {
                        // Log "All" view: Show ONLY "New" tickets as per request
                        filtered = filtered.filter { it.status.uppercase() == "NEW" || it.status.uppercase() == "PENDING" }
                    }
                }

                // 2. Type Filter (Takeaway, Dine In)
                if (typeFilter != "All") {
                    filtered = filtered.filter {
                        val normalizedType = it.orderType.lowercase().trim()
                            .replace("-", "").replace("_", "").replace(" ", "")
                        val normalizedFilter = typeFilter.lowercase().trim()
                            .replace("-", "").replace("_", "").replace(" ", "")
                        
                        when (normalizedFilter) {
                            "dinein" -> {
                                normalizedType == "dinein" || normalizedType == "normal" || normalizedType.isEmpty()
                            }
                            "takeaway" -> {
                                normalizedType == "takeaway" || normalizedType == "delivery" || 
                                        normalizedType == "quicksale" || normalizedType.contains("take")
                            }
                            else -> normalizedType == normalizedFilter || normalizedType.contains(normalizedFilter)
                        }
                    }
                }

                // 3. Search Query (Order ID or Table Name)
                if (query.isNotEmpty()) {
                    filtered = filtered.filter {
                        it.orderId.contains(query, ignoreCase = true) ||
                                it.tableName.contains(query, ignoreCase = true)
                    }
                }

                KitchenOrderUiState.Success(filtered)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KitchenOrderUiState.Loading)

    private data class DataBundle(
        val orders: List<KitchenOrderDetailData>,
        val status: String,
        val type: String,
        val isWorkstation: Boolean
    )

    fun startListeningOrders(managerId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.getRealtimeKitchenOrderDetailDatas(managerId)
                .catch { exception ->
                    _isLoading.value = false
                    _error.value = exception
                }
                .collect { incomingList ->
                    _isLoading.value = false
                    _error.value = null
                    _rawOrders.value = incomingList
                }
        }
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun setWorkstationContext(isWorkstation: Boolean) {
        _isWorkstationContext.value = isWorkstation
    }

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}