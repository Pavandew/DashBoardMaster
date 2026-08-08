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
 * STEP 3 VIEWMODEL (THE "WORKER")
 * Purpose: Handles logic and validation for Step 3 (Tax & Compliance).
 */
class Step3ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step3ViewModel"
    }

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    /**
     * Initializes the form fields for Step 3 using existing data from the Bank.
     */
    fun initFields(data: RegistrationDataModel) {
        try {
            Log.d(TAG, "Initializing fields for Step 3. Current GST: ${data.gstNumber}")
            _formFields.value = listOf(
                FormItem.StepProgress("STEP 3 OF 6", "Tax & Compliance", "GST, PAN, FSSAI and bill tax."),
                
                FormItem.SectionHeader("TAX REGISTRATION", isOptional = true, sectionNumber = "1"),
                FormItem.InputField("gst_number", "GST number", "e.g. 29ABCDE1234F1Z5", InputType.TYPE_CLASS_TEXT, value = data.gstNumber),
                FormItem.InputField("pan_number", "PAN number", "e.g. ABCDE1234F", InputType.TYPE_CLASS_TEXT, value = data.panNumber),

                FormItem.SectionHeader("TAX ON BILLS", sectionNumber = "2"),
                FormItem.SwitchField("charge_tax", "Charge tax on bills", "Adds a tax line to every order automatically.", isChecked = data.chargeTaxOnBills),
                FormItem.InputField("tax_rate", "Default tax rate (%)", "5", InputType.TYPE_CLASS_NUMBER, value = data.defaultTaxRate),
                FormItem.SwitchField("includes_tax", "Price includes tax", "Tax added on top", isChecked = data.priceIncludesTax),

                FormItem.SectionHeader("FSSAI LICENCE", isOptional = true, sectionNumber = "3"),
                FormItem.InputField("fssai_number", "FSSAI number", "e.g. 12345678901234", InputType.TYPE_CLASS_NUMBER, helperText = "14-digit food licence number.", value = data.fssaiNumber),
                FormItem.DatePickerField("fssai_expiry", "Expiry date", "dd-mm-yyyy", value = data.fssaiExpiryDate)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 3 fields", e)
        }
    }

    /**
     * Validates compliance information using the central FormValidator.
     */
    fun validate(): Boolean {
        try {
            Log.d(TAG, "Executing Step 3 validation...")
            var isValid = true
            val currentFields = _formFields.value.toMutableList()

            currentFields.forEachIndexed { index, item ->
                if (item is FormItem.InputField) {
                    val error = when (item.key) {
                        "gst_number" -> FormValidator.validateGst(item.value)
                        "pan_number" -> FormValidator.validatePan(item.value)
                        "fssai_number" -> FormValidator.validateFssai(item.value)
                        "tax_rate" -> {
                            if (item.value.isNotEmpty() && item.value.toIntOrNull() == null) "Must be a number" else null
                        }
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
                Log.i(TAG, "Step 3 validation successful.")
                _uiState.value = RegistrationUiState.Success("Step 3 Validated", "")
            } else {
                Log.e(TAG, "Step 3 validation failed.")
                _formFields.value = currentFields
                _uiState.value = RegistrationUiState.Error("Please fix the errors in the form")
            }

            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Step 3 validation", e)
            _uiState.value = RegistrationUiState.Error("An error occurred during validation")
            return false
        }
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
