package com.example.masterdashboard.manager_single_res_dash.settings.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.TaxSettings
import com.example.masterdashboard.manager_single_res_dash.settings.repo.TaxSettingsRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaxSettingsViewModel(
    private val repository: TaxSettingsRepository = TaxSettingsRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "TaxSettingsViewModel"
    }

    private val _saveState = MutableStateFlow<ResourceUiState<String>>(ResourceUiState.Idle)
    val saveState: StateFlow<ResourceUiState<String>> = _saveState.asStateFlow()

    private val _taxSettings = MutableStateFlow(TaxSettings())
    val taxSettings: StateFlow<TaxSettings> = _taxSettings.asStateFlow()

    fun loadTaxSettings(ownerUid: String, sessionManager: SessionManager) {
        if (ownerUid.isEmpty()) return

        viewModelScope.launch {
            val result = repository.getTaxSettings(ownerUid)
            result.getOrNull()?.let { settings ->
                _taxSettings.value = settings
                // Sync local SessionManager cache
                sessionManager.saveTaxSettings(
                    enabled = settings.chargeTaxOnBills,
                    rate = settings.defaultTaxRate,
                    label = settings.taxLabel,
                    priceIncludesTax = settings.priceIncludesTax
                )
            }
        }
    }

    fun saveTaxSettings(
        ownerUid: String,
        newSettings: TaxSettings,
        sessionManager: SessionManager
    ) {
        if (ownerUid.isEmpty()) {
            _saveState.value = ResourceUiState.Error("User session not found")
            return
        }

        _saveState.value = ResourceUiState.Loading

        viewModelScope.launch {
            Log.d(TAG, "Saving Tax Settings via ViewModel: rate=${newSettings.defaultTaxRate}%, label=${newSettings.taxLabel}")
            val result = repository.saveTaxSettings(ownerUid, newSettings)

            result.fold(
                onSuccess = {
                    _taxSettings.value = newSettings

                    // 1. Update local SessionManager tax cache
                    sessionManager.saveTaxSettings(
                        enabled = newSettings.chargeTaxOnBills,
                        rate = newSettings.defaultTaxRate,
                        label = newSettings.taxLabel,
                        priceIncludesTax = newSettings.priceIncludesTax
                    )

                    // 2. Update local cached restaurant details
                    val cachedDetails = sessionManager.getCachedRestaurantDetails()
                    if (cachedDetails != null) {
                        cachedDetails.chargeTaxOnBills = newSettings.chargeTaxOnBills
                        cachedDetails.defaultTaxRate = newSettings.defaultTaxRate.toString()
                        cachedDetails.gstNumber = newSettings.gstNumber
                        cachedDetails.priceIncludesTax = newSettings.priceIncludesTax
                        cachedDetails.taxSettings = newSettings
                        sessionManager.saveRestaurantDetails(cachedDetails)
                    }

                    _saveState.value = ResourceUiState.Success("Tax settings updated successfully!")
                },
                onFailure = { error ->
                    Log.e(TAG, "Error saving tax settings in ViewModel", error)
                    _saveState.value = ResourceUiState.Error(error.localizedMessage ?: "Failed to save settings")
                }
            )
        }
    }

    fun resetSaveState() {
        _saveState.value = ResourceUiState.Idle
    }
}