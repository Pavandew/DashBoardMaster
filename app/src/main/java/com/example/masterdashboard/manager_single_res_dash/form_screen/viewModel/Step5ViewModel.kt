package com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.FormItem
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * STEP 5 VIEWMODEL (THE "WORKER")
 * Purpose: Handles logic for Step 5 (Branding).
 */
class Step5ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step5ViewModel"
    }

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    /**
     * Initializes the form fields for Step 5.
     */
    fun initFields(data: RegistrationDataModel) {
        try {
            Log.d(TAG, "Initializing fields for Step 5.")
            _formFields.value = listOf(
                FormItem.StepProgress("STEP 5 OF 6", "Branding", "Logo, primary colour and theme."),
                
                FormItem.UploadField(
                    "logo", 
                    "RESTAURANT LOGO", 
                    "PNG or JPG - up to 2 MB - resized automatically.",
                    imageUri = data.restaurantLogoUri
                ),
                
                FormItem.SwitchField(
                    "show_logo", 
                    "Show logo on printed receipts", 
                    "Logo will be printed", 
                    isChecked = data.showLogoOnReceipts
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 5 fields", e)
        }
    }

    /**
     * Validates branding information.
     */
    fun validate(): Boolean {
        try {
            Log.d(TAG, "Executing Step 5 validation...")
            _uiState.value = RegistrationUiState.Success("Step 5 Validated", "")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Step 5 validation", e)
            _uiState.value = RegistrationUiState.Error("An error occurred during validation")
            return false
        }
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
