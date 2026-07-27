package com.example.masterdashboard.staff_dash.waiter_screens.order.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderCardData
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrderStatus
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.ActiveOrdersUiState
import com.example.masterdashboard.staff_dash.waiter_screens.order.models.OrderStatusFilterData
import com.example.masterdashboard.staff_dash.waiter_screens.order.repo.ActiveOrdersRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ActiveOrdersViewModel(
    private val repository: ActiveOrdersRepository
) : ViewModel() {

    companion object {
        private const val TAG = "Order_Flow_Debug"
    }

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

    private var streamJob: Job? = null

    init {
        Log.d(TAG, "🏗️ [VIEWMODEL] ActiveOrdersViewModel initialized.")
        updateUiStateFiltersAndOrders()
    }

    fun streamActiveOrders(managerId: String?) {
        // ALWAYS reset selection to "All" (ID "1") whenever navigating into this screen
        currentlySelectedFilterId = "1"

        // If stream is already actively collecting Firestore snapshots, don't recreate the listener,
        // just update the UI state to select "All" from the existing cache!
        if (streamJob?.isActive == true) {
            Log.d(TAG, "🏗️ [VIEWMODEL] Stream active & cached. Resetting filter to 'All' and serving instantly!")
            updateUiStateFiltersAndOrders()
            return
        }

        Log.d(TAG, "🏗️ [VIEWMODEL] streamActiveOrders() called with Manager ID: $managerId")

        if (managerId.isNullOrEmpty()) {
            Log.e(TAG, "🏗️ [VIEWMODEL] Invalid Manager ID! Session error.")
            _uiState.update { it.copy(isLoading = false, errorMessage = "Session Error: Invalid Manager ID") }
            return
        }

        if (masterOrdersCache.isEmpty()) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }

        streamJob = repository.fetchLiveActiveOrders(managerId).onEach { resourceUiState ->
            when (resourceUiState) {
                is ResourceUiState.Loading -> {
                    if (masterOrdersCache.isEmpty()) {
                        Log.d(TAG, "🏗️ [VIEWMODEL] State -> Loading...")
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }

                is ResourceUiState.Success -> {
                    Log.i(TAG, "🏗️ [VIEWMODEL] State -> Success! Received ${resourceUiState.data.size} items from repository.")
                    masterOrdersCache = resourceUiState.data

                    masterOrdersCache.forEachIndexed { i, order ->
                        Log.d(TAG, "🏗️ [VIEWMODEL] Cached Order [$i]: ID='${order.orderId}', Table='${order.tableName}', Items=${order.totalItems}, Time='${order.orderTime}', Status=${order.status}")
                    }

                    updateUiStateFiltersAndOrders()
                }

                is ResourceUiState.Error -> {
                    Log.e(TAG, "🏗️ [VIEWMODEL] State -> Error: ${resourceUiState.message}")
                    _uiState.update { it.copy(isLoading = false, errorMessage = resourceUiState.message) }
                }

                ResourceUiState.Idle -> {
                    // Do nothing
                }
            }
        }.launchIn(viewModelScope)
    }

    fun selectFilterCategory(filterId: String) {
        Log.d(TAG, "🏗️ [VIEWMODEL] selectFilterCategory() clicked -> Filter ID: $filterId")
        currentlySelectedFilterId = filterId
        updateUiStateFiltersAndOrders()
    }

    fun resetFilterToAll() {
        Log.d(TAG, "🏗️ [VIEWMODEL] Resetting active filter back to 'All' (ID: 1)")
        currentlySelectedFilterId = "1"
        updateUiStateFiltersAndOrders()
    }

    private fun updateUiStateFiltersAndOrders() {
        val totalAll = masterOrdersCache.size
        val totalPending = masterOrdersCache.count { it.status == ActiveOrderStatus.PENDING }
        val totalPreparing = masterOrdersCache.count { it.status == ActiveOrderStatus.PREPARING }
        val totalReady = masterOrdersCache.count { it.status == ActiveOrderStatus.READY }
        val totalServed = masterOrdersCache.count { it.status == ActiveOrderStatus.SERVED }
        val totalPaid = masterOrdersCache.count { it.status == ActiveOrderStatus.PAID }

        Log.d(TAG, "🏗️ [VIEWMODEL] Filter Counts -> All: $totalAll, Pending: $totalPending, Preparing: $totalPreparing, Ready: $totalReady, Served: $totalServed, Paid: $totalPaid")

        val computedFilters = listOf(
            OrderStatusFilterData("1", "All ($totalAll)", null, currentlySelectedFilterId == "1"),
            OrderStatusFilterData(
                "2",
                "Pending ($totalPending)",
                ActiveOrderStatus.PENDING,
                currentlySelectedFilterId == "2"
            ),
            OrderStatusFilterData(
                "3",
                "Preparing ($totalPreparing)",
                ActiveOrderStatus.PREPARING,
                currentlySelectedFilterId == "3"
            ),
            OrderStatusFilterData(
                "4",
                "Ready ($totalReady)",
                ActiveOrderStatus.READY,
                currentlySelectedFilterId == "4"
            ),
            OrderStatusFilterData(
                "5",
                "Served ($totalServed)",
                ActiveOrderStatus.SERVED,
                currentlySelectedFilterId == "5"
            ),
            OrderStatusFilterData(
                "6",
                "Paid ($totalPaid)",
                ActiveOrderStatus.PAID,
                currentlySelectedFilterId == "6"
            )
        )

        val currentActiveFilterType = computedFilters.firstOrNull { it.isSelected }?.statusType

        val filteredOrdersList = if (currentActiveFilterType == null) {
            masterOrdersCache
        } else {
            masterOrdersCache.filter { it.status == currentActiveFilterType }
        }

        Log.i(TAG, "🏗️ [VIEWMODEL] Pushing ${filteredOrdersList.size} visible orders to Fragment UI state.")

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