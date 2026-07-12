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
    private val _selectedStatusFilter = MutableStateFlow("All")

    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    // Cleanly fuses data stream and selected tag criteria instantly
    val uiState: StateFlow<KitchenOrderUiState> = combine(
        _rawOrders,
        _selectedStatusFilter
    ) { rawList, filter ->
        if (rawList.isEmpty() && _uiStateInitCheck.value) {
            KitchenOrderUiState.Loading
        } else {
            val filtered = if (filter == "All") rawList else rawList.filter {
                it.status.equals(
                    filter,
                    ignoreCase = true
                )
            }
            KitchenOrderUiState.Success(filtered)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KitchenOrderUiState.Loading)

    private val _uiStateInitCheck = MutableStateFlow(true)

    init {
        fetchLiveOrders()
    }

    private fun fetchLiveOrders() {
        viewModelScope.launch {
            repository.getRealtimeKitchenOrderDetailDatas()
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