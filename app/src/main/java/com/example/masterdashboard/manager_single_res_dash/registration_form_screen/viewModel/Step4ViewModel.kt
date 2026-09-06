package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.FormItem
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.uiState.RegistrationUiState
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.utils.FormValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * STEP 4 VIEWMODEL (THE "WORKER")
 * Purpose: Handles logic and validation for Step 4 (Billing).
 */
class Step4ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step4ViewModel"
    }

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    /**
     * Initializes the form fields for Step 4.
     */
    fun initFields(data: RegistrationDataModel) {
        try {
            Log.d(TAG, "Initializing fields for Step 4. Current Currency: ${data.currency}")
            _formFields.value = listOf(
                FormItem.StepProgress("STEP 4 OF 6", "Billing", "Currency, invoices and print size."),
                
                FormItem.SectionHeader("CURRENCY & LANGUAGE", sectionNumber = "1"),
                FormItem.DropdownField(
                    "currency", 
                    "Currency *", 
                    "Select currency", 
                    listOf("₹ - Indian Rupee (INR)", "$ - US Dollar (USD)", "€ - Euro (EUR)"),
                    selectedValue = data.currency
                ),
                FormItem.InputField("currency_symbol", "Currency symbol *", "₹", android.text.InputType.TYPE_CLASS_TEXT, value = data.currencySymbol),
                FormItem.DropdownField(
                    "language", 
                    "Language *", 
                    "Select language", 
                    listOf("English", "Hindi", "Spanish"),
                    selectedValue = data.language
                ),

                FormItem.SectionHeader("INVOICE CONFIGURATION", sectionNumber = "2"),
                FormItem.InputField("invoice_prefix", "Invoice prefix", "INV-", android.text.InputType.TYPE_CLASS_TEXT, value = data.invoicePrefix),
                FormItem.InputField("invoice_start", "Starting invoice number", "1", android.text.InputType.TYPE_CLASS_NUMBER, value = data.startingInvoiceNumber),
                FormItem.DropdownField(
                    "print_size", 
                    "Print size", 
                    "Select print size", 
                    listOf("80 mm thermal", "58 mm thermal", "A4 standard"),
                    selectedValue = data.printSize
                ),
                
                FormItem.InfoCard("Bank account, UPI and the receipt footer can be added later from Settings → Restaurant profile.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 4 fields", e)
        }
    }

    /**
     * Validates billing information.
     */
    fun validate(): Boolean {
        try {
            Log.d(TAG, "Executing Step 4 validation...")
            var isValid = true
            val currentFields = _formFields.value.toMutableList()

            currentFields.forEachIndexed { index, item ->
                if (item is FormItem.InputField) {
                    val error = when (item.key) {
                        "currency_symbol" -> FormValidator.validateNotEmpty(item.value, "Currency symbol")
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
                Log.i(TAG, "Step 4 validation successful.")
                _uiState.value = RegistrationUiState.Success("Step 4 Validated", "")
            } else {
                Log.e(TAG, "Step 4 validation failed.")
                _formFields.value = currentFields
                _uiState.value = RegistrationUiState.Error("Please fix the errors in the form")
            }

            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Step 4 validation", e)
            _uiState.value = RegistrationUiState.Error("An error occurred during validation")
            return false
        }
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
