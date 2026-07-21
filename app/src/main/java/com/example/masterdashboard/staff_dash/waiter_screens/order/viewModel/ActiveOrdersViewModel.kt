package com.example.masterdashboard.staff_dash.waiter_screens.order.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrdersUiState
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderStatusFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.order.repo.ActiveOrdersRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ActiveOrdersViewModel(
    private val repository: ActiveOrdersRepository
) : ViewModel() {

    class ActiveOrdersViewModelFactory(private val repository: ActiveOrdersRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ActiveOrdersViewModel::class.java)) {
                return ActiveOrdersViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val _uiState = MutableStateFlow(ActiveOrdersUiState())
    val uiState: StateFlow<ActiveOrdersUiState> = _uiState

    private var masterOrdersCache: List<ActiveOrderCardData> = emptyList()
    private var currentlySelectedFilterId: String = "1"

    init {
        updateUiStateFiltersAndOrders()
    }

    fun streamActiveOrders(managerId: String?) {
        if (managerId.isNullOrEmpty()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Session Error: Invalid Manager ID") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        repository.fetchLiveActiveOrders(managerId).onEach { resourceUiState ->
            when(resourceUiState) {
                is ResourceUiState.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }

                is ResourceUiState.Success -> {
                    masterOrdersCache = resourceUiState.data
                    updateUiStateFiltersAndOrders()
                }

                is ResourceUiState.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = resourceUiState.message) }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun selectFilterCategory(filterId: String) {
        currentlySelectedFilterId = filterId
        updateUiStateFiltersAndOrders()
    }

    private fun updateUiStateFiltersAndOrders() {
        val totalAll = masterOrdersCache.size
        val totalPreparing = masterOrdersCache.count { it.status == ActiveOrderStatus.PREPARING }
        val totalReady = masterOrdersCache.count { it.status == ActiveOrderStatus.READY }
        val totalServed = masterOrdersCache.count { it.status == ActiveOrderStatus.SERVED }

        val computedFilters = listOf(
            OrderStatusFilterData("1", "All ($totalAll)", null, currentlySelectedFilterId == "1"),
            OrderStatusFilterData(
                "2",
                "Preparing ($totalPreparing)",
                ActiveOrderStatus.PREPARING,
                currentlySelectedFilterId == "2"
            ),
            OrderStatusFilterData(
                "3",
                "Ready ($totalReady)",
                ActiveOrderStatus.READY,
                currentlySelectedFilterId == "3"
            ),
            OrderStatusFilterData(
                "4",
                "Served ($totalServed)",
                ActiveOrderStatus.SERVED,
                currentlySelectedFilterId == "4"
            )
        )

        val currentActiveFilterType = computedFilters.firstOrNull { it.isSelected }?.statusType

        val filteredOrdersList = if (currentActiveFilterType == null) {
            masterOrdersCache
        } else {
            masterOrdersCache.filter { it.status == currentActiveFilterType }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                filters = computedFilters,
                visibleOrders = filteredOrdersList,
                errorMessage = null
            )
        }
    }
}