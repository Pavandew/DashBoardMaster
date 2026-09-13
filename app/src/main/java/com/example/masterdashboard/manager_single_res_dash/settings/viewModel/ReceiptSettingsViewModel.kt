package com.example.masterdashboard.manager_single_res_dash.settings.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.BillingPrinterSettings
import com.example.masterdashboard.manager_single_res_dash.settings.repo.ReceiptSettingsRepository
import com.example.masterdashboard.staff_dash.waiter_screens.table.uistate.ResourceUiState
import com.example.masterdashboard.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReceiptSettingsViewModel(
    private val repository: ReceiptSettingsRepository = ReceiptSettingsRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "ReceiptSettingsVM"
    }

    private val _saveState = MutableStateFlow<ResourceUiState<String>>(ResourceUiState.Idle)
    val saveState: StateFlow<ResourceUiState<String>> = _saveState.asStateFlow()

    private val _receiptSettings = MutableStateFlow(BillingPrinterSettings())
    val receiptSettings: StateFlow<BillingPrinterSettings> = _receiptSettings.asStateFlow()

    fun loadReceiptSettings(ownerUid: String) {
        if (ownerUid.isEmpty()) return

        viewModelScope.launch {
            val result = repository.getReceiptSettings(ownerUid)
            result.getOrNull()?.let { settings ->
                _receiptSettings.value = settings
            }
        }
    }

    fun saveReceiptSettings(
        ownerUid: String,
        newSettings: BillingPrinterSettings,
        sessionManager: SessionManager
    ) {
        if (ownerUid.isEmpty()) {
            _saveState.value = ResourceUiState.Error("User session not found")
            return
        }

        _saveState.value = ResourceUiState.Loading

        viewModelScope.launch {
            Log.d(TAG, "Saving Receipt Settings via ViewModel: prefix=${newSettings.invoicePrefix}, size=${newSettings.printSize}")
            val result = repository.saveReceiptSettings(ownerUid, newSettings)

            result.fold(
                onSuccess = {
                    _receiptSettings.value = newSettings

                    // Update local cached restaurant details
                    val cachedDetails = sessionManager.getCachedRestaurantDetails()
                    if (cachedDetails != null) {
                        cachedDetails.invoicePrefix = newSettings.invoicePrefix
                        cachedDetails.startingInvoiceNumber = newSettings.startingInvoiceNumber
                        cachedDetails.printSize = newSettings.printSize
                        cachedDetails.showLogoOnReceipts = newSettings.showLogoOnReceipts
                        cachedDetails.billingPrinterSettings = newSettings
                        sessionManager.saveRestaurantDetails(cachedDetails)
                    }

                    _saveState.value = ResourceUiState.Success("Receipt layout settings saved successfully!")
                },
                onFailure = { error ->
                    Log.e(TAG, "Error saving receipt settings in ViewModel", error)
                    _saveState.value = ResourceUiState.Error(error.localizedMessage ?: "Failed to save settings")
                }
            )
        }
    }

    fun resetSaveState() {
        _saveState.value = ResourceUiState.Idle
    }
}
