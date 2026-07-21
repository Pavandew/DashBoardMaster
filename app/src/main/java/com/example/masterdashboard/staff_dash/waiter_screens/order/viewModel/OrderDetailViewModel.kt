package com.example.masterdashboard.staff_dash.waiter_screens.order.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderDetailExpansionUiState
import com.example.masterdashboard.staff_dash.waiter_screens.order.repo.OrderDetailRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class OrderDetailViewModel(
    private val repository: OrderDetailRepository
) : ViewModel(){

    class OrderDetailViewModelFactory(private val repository: OrderDetailRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return OrderDetailViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel configuration")
        }
    }
    private val _uiState = MutableStateFlow(OrderDetailExpansionUiState())
    val uiState: StateFlow<OrderDetailExpansionUiState> = _uiState.asStateFlow()

    fun loadOrderSpecifications(orderId: String) {
        repository.fetchDetailedTicked(orderId).onEach { resource ->
            when (resource) {
                is ResourceUiState.Loading -> _uiState.update { it.copy(isLoading = true) }
                is ResourceUiState.Success -> _uiState.value = resource.data
                is ResourceUiState.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = resource.message) }
            }
        }.launchIn(viewModelScope)
    }

    fun finalizeOrderAsServed(orderId: String, onCompletionSuccess: () -> Unit) {
        repository.updateOrderStatusToServed(orderId).onEach { resource ->
            when (resource) {
                is ResourceUiState.Loading -> _uiState.update { it.copy(isLoading = true) }
                is ResourceUiState.Success -> {
                    _uiState.update { it.copy(isLoading = false, status = ActiveOrderStatus.SERVED) }
                    onCompletionSuccess()
                }
                is ResourceUiState.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = resource.message) }
                }
            }
        }.launchIn(viewModelScope)
    }
}