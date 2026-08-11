package com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel

import android.text.InputType
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.FormItem
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.form_screen.utils.FormValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * STEP 1 VIEWMODEL (THE "WORKER")
 * Purpose: Handles logic, field generation, and validation specifically for Step 1 (Owner & Restaurant).
 */
class Step1ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step1ViewModel"
    }

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    /**
     * Initializes the form fields for Step 1 using existing data from the Bank.
     */
    fun initFields(data: RegistrationDataModel) {
        try {
            Log.d(TAG, "Initializing fields for Step 1 with current data: ${data.restaurantName}")
            _formFields.value = listOf(
                FormItem.StepProgress("STEP 1 OF 6", "Owner & Restaurant", "Who runs it and what it's called."),
                FormItem.InfoCard("The owner is shown on invoices and internal reports — never to customers."),
                FormItem.SectionHeader("OWNER INFORMATION", sectionNumber = "1"),
                FormItem.InputField("owner_name", "Owner full name *", "e.g. John Doe", InputType.TYPE_CLASS_TEXT, value = data.ownerFullName),
                FormItem.InputField("owner_email", "Owner email *", "e.g. owner@gmail.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, value = data.ownerEmail),
                FormItem.InputField("owner_mobile", "Owner mobile *", "e.g. 98765 43210", InputType.TYPE_CLASS_PHONE, helperText = "10-digit mobile — used for account recovery.", value = data.ownerMobile),
                FormItem.SectionHeader("RESTAURANT INFORMATION", sectionNumber = "2"),
                FormItem.InputField("res_name", "Restaurant name *", "e.g. The Grand Bistro", InputType.TYPE_CLASS_TEXT, helperText = "Shown to customers everywhere.", value = data.restaurantName),
                FormItem.InputField("business_type", "Business type *", "e.g. Casual Dining", InputType.TYPE_CLASS_TEXT, value = data.businessType),
                FormItem.InputField("legal_name", "Legal / registered name", "e.g. ABC Hospitality Pvt Ltd", InputType.TYPE_CLASS_TEXT, helperText = "As per legal documents.", value = data.legalName),
                FormItem.InputField("display_name", "Display name (receipts)", "e.g. Grand Bistro", InputType.TYPE_CLASS_TEXT, helperText = "Short version printed on bills.", value = data.displayName)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 1 fields", e)
        }
    }

    /**
     * Validates the fields in Step 1.
     */
    fun validate(): Boolean {
        try {
            Log.d(TAG, "Executing Step 1 validation...")
            var isValid = true
            val currentFields = _formFields.value.toMutableList()

            currentFields.forEachIndexed { index, item ->
                if (item is FormItem.InputField) {
                    val error = when (item.key) {
                        "owner_name" -> FormValidator.validateNotEmpty(item.value, "Owner name")
                        "owner_email" -> FormValidator.validateEmail(item.value)
                        "owner_mobile" -> FormValidator.validatePhone(item.value)
                        "res_name" -> FormValidator.validateNotEmpty(item.value, "Restaurant name")
                        "business_type" -> FormValidator.validateNotEmpty(item.value, "Business type")
                        else -> null
                    }

                    if (error != null) {
                        Log.w(TAG, "Validation failed for key: ${item.key} | Error: $error")
                        isValid = false
                        currentFields[index] = item.copy(error = error)
                    } else {
                        currentFields[index] = item.copy(error = null)
                    }
                }
            }

            if (isValid) {
                Log.i(TAG, "Step 1 validation successful.")
                _uiState.value = RegistrationUiState.Success("Step 1 Validated", "")
            } else {
                Log.e(TAG, "Step 1 validation failed. Red warnings triggered.")
                _formFields.value = currentFields
                _uiState.value = RegistrationUiState.Error("Please fix the errors in the form")
            }

            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Step 1 validation", e)
            _uiState.value = RegistrationUiState.Error("An error occurred during validation")
            return false
        }
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
