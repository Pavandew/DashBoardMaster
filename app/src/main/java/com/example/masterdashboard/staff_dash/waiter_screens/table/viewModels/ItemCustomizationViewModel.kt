package com.example.masterdashboard.staff_dash.waiter_screens.table.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.waiter_screens.table.models.AddonItem
import com.example.masterdashboard.staff_dash.waiter_screens.table.repo.ItemCustomizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ItemCustomizationViewModel(private val repository: ItemCustomizationRepository = ItemCustomizationRepository()) : ViewModel() {

    private val _addonsState = MutableStateFlow<Map<String, List<AddonItem>>>(emptyMap())
    val addonsState: StateFlow<Map<String, List<AddonItem>>> = _addonsState.asStateFlow()

    private val _isLoadingAddons = MutableStateFlow(false)
    val isLoadingAddons: StateFlow<Boolean> = _isLoadingAddons.asStateFlow()

    fun loadAddons(managerId: String, categoryId: String, itemId: String) {
        if (_addonsState.value.containsKey(itemId)) return

        viewModelScope.launch {
            _isLoadingAddons.value = true
            val addons = repository.getAddonsForItem(managerId, categoryId, itemId)
            val currentMap = _addonsState.value.toMutableMap()
            currentMap[itemId] = addons
            _addonsState.value = currentMap
            _isLoadingAddons.value = false
        }
    }
    
    fun getAddonsForItem(itemId: String): List<AddonItem> {
        return _addonsState.value[itemId] ?: emptyList()
    }
}
