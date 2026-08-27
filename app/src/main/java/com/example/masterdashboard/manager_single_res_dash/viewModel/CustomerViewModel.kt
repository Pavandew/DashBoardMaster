package com.example.masterdashboard.manager_single_res_dash.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.models.CustomerModel
import com.example.masterdashboard.manager_single_res_dash.repo.CustomerRepository
import com.example.masterdashboard.manager_single_res_dash.uistate.CustomerUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CustomerViewModel : ViewModel() {

    private val repository = CustomerRepository()

    private val _customers = MutableStateFlow<List<CustomerModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private var isObserving = false

    val uiState: StateFlow<CustomerUiState> = combine(_customers, _searchQuery, _isLoading) { list, query, loading ->
        val filtered = if (query.isEmpty()) {
            list
        } else {
            list.filter { 
                it.customerName.contains(query, ignoreCase = true) || 
                it.customerMobile.contains(query) 
            }
        }
        CustomerUiState(customers = filtered, isLoading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CustomerUiState(isLoading = true))

    fun observeCustomers(managerId: String) {
        if (isObserving) return
        isObserving = true

        _isLoading.value = true
        viewModelScope.launch {
            repository.getCustomers(managerId)
                .catch { e ->
                    _isLoading.value = false
                    isObserving = false
                }
                .collect { list ->
                    _customers.value = list
                    _isLoading.value = false
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}