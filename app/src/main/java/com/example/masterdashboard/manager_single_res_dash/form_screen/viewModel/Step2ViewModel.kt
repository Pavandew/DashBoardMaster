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
 * STEP 2 VIEWMODEL (THE "WORKER")
 * Purpose: Handles logic, field generation, and validation specifically for Step 2 (Address & Contact).
 */
class Step2ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step2ViewModel"
    }

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    /**
     * Initializes the form fields for Step 2 using existing data from the Bank.
     */
    fun initFields(data: RegistrationDataModel) {
        try {
            Log.d(TAG, "Initializing fields for Step 2 with current address: ${data.address}")
            _formFields.value = listOf(
                FormItem.StepProgress("STEP 2 OF 6", "Address & Contact", "Where you are and how to reach you."),
                FormItem.SectionHeader("RESTAURANT ADDRESS", sectionNumber = "1"),
                FormItem.InputField("address", "Address *", "e.g. Marathahalli", InputType.TYPE_CLASS_TEXT, value = data.address),
                FormItem.InputField("landmark", "Landmark", "Nearby landmark", InputType.TYPE_CLASS_TEXT, value = data.landmark),
                FormItem.InputField("pin_code", "PIN code *", "e.g. 560037", InputType.TYPE_CLASS_NUMBER, helperText = "6 digits — we'll fill the rest for you.", value = data.pinCode),
                FormItem.InputField("city", "City *", "e.g. Banglore", InputType.TYPE_CLASS_TEXT, value = data.city),
                FormItem.InputField("state", "State *", "e.g. Karnataka", InputType.TYPE_CLASS_TEXT, value = data.state),
                FormItem.InputField("country", "Country *", "e.g. India", InputType.TYPE_CLASS_TEXT, value = data.country),
                FormItem.SectionHeader("RESTAURANT CONTACT", sectionNumber = "2"),
                FormItem.InputField("contact_number", "Contact number *", "e.g. 1234567890", InputType.TYPE_CLASS_PHONE, helperText = "Primary number printed on receipts.", value = data.contactNumber),
                FormItem.InputField("contact_email", "Contact email *", "e.g. abc@gmail.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, value = data.contactEmail),
                FormItem.InputField("whatsapp", "WhatsApp", "e.g. 98765 43210", InputType.TYPE_CLASS_PHONE, value = data.whatsappNumber),
                FormItem.InputField("website", "Website", "e.g. https://...", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI, value = data.website)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 2 fields", e)
        }
    }

    /**
     * Validates Step 2 fields and triggers red warnings if validation fails.
     */
    fun validate(): Boolean {
        try {
            Log.d(TAG, "Executing Step 2 validation...")
            var isValid = true
            val currentFields = _formFields.value.toMutableList()

            currentFields.forEachIndexed { index, item ->
                if (item is FormItem.InputField) {
                    val error = when (item.key) {
                        "address" -> FormValidator.validateNotEmpty(item.value, "Address")
                        "pin_code" -> FormValidator.validatePinCode(item.value)
                        "city" -> FormValidator.validateNotEmpty(item.value, "City")
                        "state" -> FormValidator.validateNotEmpty(item.value, "State")
                        "country" -> FormValidator.validateNotEmpty(item.value, "Country")
                        "contact_number" -> FormValidator.validatePhone(item.value)
                        "contact_email" -> FormValidator.validateEmail(item.value)
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
                Log.i(TAG, "Step 2 validation successful.")
                _uiState.value = RegistrationUiState.Success("Step 2 Validated", "")
            } else {
                Log.e(TAG, "Step 2 validation failed. Red warnings triggered.")
                _formFields.value = currentFields
                _uiState.value = RegistrationUiState.Error("Please fix the errors in the form")
            }

            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Step 2 validation", e)
            _uiState.value = RegistrationUiState.Error("An error occurred during validation")
            return false
        }
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
