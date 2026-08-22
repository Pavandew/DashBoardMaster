package com.example.masterdashboard.staff_dash.kitchen_screens.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.kitchen_screens.model.KitchenOrderDetailData
import com.example.masterdashboard.staff_dash.kitchen_screens.repo.KitchenOrderRepository
import com.example.masterdashboard.staff_dash.kitchen_screens.uistate.KitchenOrderUiState
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.TableFilterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the kitchen's main order list and workstation feed.
 * Handles dual context: New Order Log and Active Workstation.
 */
class KitchenOrderViewModel(private val repository: KitchenOrderRepository = KitchenOrderRepository()) : ViewModel() {

    companion object {
        private const val TAG = "KitchenOrderVM"
    }

    private val _rawOrders = MutableStateFlow<List<KitchenOrderDetailData>>(emptyList())
    private val _selectedStatusFilter = MutableStateFlow("All")
    private val _selectedTypeFilter = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _isWorkstationContext = MutableStateFlow(false)
    private val _error = MutableStateFlow<Throwable?>(null)
    private val _isLoading = MutableStateFlow(false)
    private var isListening = false

    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    /**
     * Reactive pipeline that fuses the raw Firestore stream with local UI filters (Search, Status, Role).
     */
    val uiState: StateFlow<KitchenOrderUiState> = combine(
        _rawOrders,
        _selectedStatusFilter,
        _selectedTypeFilter,
        _searchQuery,
        combine(_isWorkstationContext, _isLoading, _error) { workstation, loading, error ->
            Triple(workstation, loading, error)
        }
    ) { rawList, statusFilter, typeFilter, query, triple ->
        val (isWorkstation, loading, error) = triple

        if (error != null) {
            return@combine KitchenOrderUiState.Error(error)
        }

        if (loading && rawList.isEmpty() && !isListening) {
            return@combine KitchenOrderUiState.Loading
        }

        // 1. Pre-filter finalized or irrelevant orders
        var baseOrders = rawList.filter {
            val s = it.status.uppercase()
            s != "PAID" && s != "REJECTED"
        }

        val computedFilters = mutableListOf<TableFilterData>()

        if (isWorkstation) {
            // --- WORKSTATION CONTEXT (Preparing/Ready/Served) ---
            val workstationBase = baseOrders.filter { it.status.uppercase() != "NEW" && it.status.uppercase() != "PENDING" }
            
            val countAll = workstationBase.size
            val countPreparing = workstationBase.count { it.status.equals("Preparing", ignoreCase = true) }
            val countReady = workstationBase.count { it.status.equals("Ready", ignoreCase = true) }
            val countCompleted = workstationBase.count { it.status.equals("Completed", ignoreCase = true) || it.status.equals("Served", ignoreCase = true) }

            computedFilters.add(TableFilterData("1", "All ($countAll)", statusFilter == "All"))
            computedFilters.add(TableFilterData("2", "Preparing ($countPreparing)", statusFilter == "Preparing"))
            computedFilters.add(TableFilterData("3", "Ready ($countReady)", statusFilter == "Ready"))
            computedFilters.add(TableFilterData("4", "Completed ($countCompleted)", statusFilter == "Completed"))

            // Apply active status filter
            baseOrders = if (statusFilter != "All") {
                baseOrders.filter { it.status.equals(statusFilter, ignoreCase = true) }
            } else {
                workstationBase
            }
        } else {
            // --- LOG CONTEXT (New Orders only) ---
            val logBase = baseOrders.filter { it.status.uppercase() == "NEW" || it.status.uppercase() == "PENDING" }
            
            val countAll = logBase.size
            val countTakeaway = logBase.count { 
                val t = it.orderType.lowercase()
                t.contains("take") || t.contains("delivery") || t.contains("quick")
            }
            val countDineIn = logBase.count { 
                val t = it.orderType.lowercase()
                t == "dinein" || t == "normal" || t.isEmpty()
            }

            computedFilters.add(TableFilterData("1", "All ($countAll)", typeFilter == "All"))
            computedFilters.add(TableFilterData("2", "Takeaway ($countTakeaway)", typeFilter == "Takeaway"))
            computedFilters.add(TableFilterData("3", "Dine In ($countDineIn)", typeFilter == "Dine In"))

            // Apply active type filter
            baseOrders = if (typeFilter != "All") {
                logBase.filter {
                    val normalizedType = it.orderType.lowercase().trim().replace("-", "").replace("_", "").replace(" ", "")
                    val normalizedFilter = typeFilter.lowercase().trim().replace("-", "").replace("_", "").replace(" ", "")
                    
                    when (normalizedFilter) {
                        "dinein" -> normalizedType == "dinein" || normalizedType == "normal" || normalizedType.isEmpty()
                        "takeaway" -> normalizedType == "takeaway" || normalizedType == "delivery" || normalizedType == "quicksale" || normalizedType.contains("take")
                        else -> normalizedType == normalizedFilter
                    }
                }
            } else {
                logBase
            }
        }

        // 2. Apply Search Query (Matches Order ID or Table Name)
        if (query.isNotEmpty()) {
            baseOrders = baseOrders.filter {
                it.orderId.contains(query, ignoreCase = true) ||
                        it.tableName.contains(query, ignoreCase = true)
            }
        }

        KitchenOrderUiState.Success(baseOrders, computedFilters)
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KitchenOrderUiState.Loading)

    /**
     * Initializes the real-time Firestore stream for the restaurant.
     */
    fun startListeningOrders(managerId: String) {
        if (managerId.isEmpty()) {
            Log.e(TAG, "startListeningOrders: Manager ID is empty. Stream cannot be started.")
            return
        }
        
        // Prevent multiple simultaneous listeners or unnecessary restarts
        if (isListening) {
            Log.d(TAG, "startListeningOrders: Already listening. Skipping flow restart.")
            return
        }

        Log.i(TAG, "startListeningOrders: Opening real-time stream for manager: $managerId")
        _isLoading.value = true
        viewModelScope.launch {
            isListening = true
            repository.getRealtimeKitchenOrderDetailDatas(managerId)
                .catch { exception ->
                    Log.e(TAG, "Firestore Stream: Error encountered", exception)
                    _isLoading.value = false
                    _error.value = exception
                }
                .collect { incomingList ->
                    Log.d(TAG, "Firestore Stream: Received update with ${incomingList.size} documents.")
                    _isLoading.value = false
                    _error.value = null
                    _rawOrders.value = incomingList
                }
        }
    }

    fun setStatusFilter(status: String) {
        Log.d(TAG, "Filter Change: Status -> '$status'")
        _selectedStatusFilter.value = status
    }

    fun setWorkstationContext(isWorkstation: Boolean) {
        Log.d(TAG, "Context Change: Workstation Mode -> $isWorkstation")
        _isWorkstationContext.value = isWorkstation
    }

    fun setTypeFilter(type: String) {
        Log.d(TAG, "Filter Change: Type -> '$type'")
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        Log.v(TAG, "Filter Change: Search Query -> '$query'")
        _searchQuery.value = query
    }
}
