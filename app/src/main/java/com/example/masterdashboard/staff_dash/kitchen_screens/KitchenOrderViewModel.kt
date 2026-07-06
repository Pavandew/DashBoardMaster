package com.example.masterdashboard.staff_dash.kitchen_screens

import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderData


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.kitchen_screens.repository.KitchenOrderRepository
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class KitchenOrderViewModel(private val repository: KitchenOrderRepository = KitchenOrderRepository()) : ViewModel() {

    private val _rawOrders = MutableStateFlow<List<KitchenOrderData>>(emptyList())
    private val _selectedStatusFilter = MutableStateFlow("All")

    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    // Cleanly fuses data stream and selected tag criteria instantly
    val uiState: StateFlow<KitchenOrderUiState> = combine(_rawOrders, _selectedStatusFilter) { rawList, filter ->
        if (rawList.isEmpty() && _uiStateInitCheck.value) {
            KitchenOrderUiState.Loading
        } else {
            val filtered = if (filter == "All") rawList else rawList.filter { it.status.equals(filter, ignoreCase = true) }
            KitchenOrderUiState.Success(filtered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KitchenOrderUiState.Loading)

    private val _uiStateInitCheck = MutableStateFlow(true)

    init {
        fetchLiveOrders()
    }

    private fun fetchLiveOrders() {
        viewModelScope.launch {
            repository.getRealtimeKitchenOrderDatas()
                .catch { exception ->
                    _uiStateInitCheck.value = false
                    // Update error state downstream
                }
                .collect { incomingList ->
                    _uiStateInitCheck.value = false
                    _rawOrders.value = incomingList
                }
        }
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }
}