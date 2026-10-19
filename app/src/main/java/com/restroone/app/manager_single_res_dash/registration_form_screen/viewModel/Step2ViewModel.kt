package com.restroone.app.manager_single_res_dash.registration_form_screen.viewModel

import android.text.InputType
import android.util.Log
import androidx.lifecycle.ViewModel
import com.restroone.app.manager_single_res_dash.registration_form_screen.model.FormItem
import com.restroone.app.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.restroone.app.manager_single_res_dash.registration_form_screen.uiState.RegistrationUiState
import com.restroone.app.manager_single_res_dash.registration_form_screen.utils.FormValidator
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
        
        private val INDIAN_STATES = listOf(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", 
            "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", 
            "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", 
            "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", 
            "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
            "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu", 
            "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
        ).sorted()

        private val COUNTRIES = listOf(
            "India", "United States", "United Kingdom", "United Arab Emirates", 
            "Canada", "Australia", "Singapore", "Germany", "France"
        ).sorted()

        private val COUNTRY_CODES = listOf(
            "+91 (IN)", "+1 (US)", "+44 (UK)", "+971 (AE)", "+61 (AU)", "+65 (SG)", "+1 (CA)", "+49 (DE)"
        )
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
            val addressInfo = data.getEffectiveAddressInfo()
            Log.d(TAG, "Initializing fields for Step 2 with current address: ${addressInfo.address}")
            
            // Logic to split country code from phone number if they exist
            val (contactCode, contactNum) = splitPhone(addressInfo.contactNumber, "+91 (IN)")
            val (whatsappCode, whatsappNum) = splitPhone(addressInfo.whatsappNumber, "+91 (IN)")
            
            val selectedCountry = if (addressInfo.country.isEmpty()) "India" else addressInfo.country

            _formFields.value = listOf(
                FormItem.StepProgress("STEP 2 OF 5", "Address & Contact", "Where you are and how to reach you."),
                
                FormItem.SectionHeader("RESTAURANT ADDRESS", sectionNumber = "1"),
                FormItem.InputField("address", "Address *", "e.g. Street, Area, Building", InputType.TYPE_CLASS_TEXT, value = addressInfo.address),
                FormItem.InputField("landmark", "Landmark", "Nearby landmark", InputType.TYPE_CLASS_TEXT, value = addressInfo.landmark),
                FormItem.InputField("pin_code", "PIN code *", "6-digit code", InputType.TYPE_CLASS_NUMBER, helperText = "We'll use this for tax calculations.", value = addressInfo.pinCode),
                
                FormItem.InputField("city", "City *", "e.g. Bangalore", InputType.TYPE_CLASS_TEXT, value = addressInfo.city),
                
                FormItem.DropdownField(
                    "state", 
                    "State *", 
                    "Select state", 
                    INDIAN_STATES, 
                    selectedValue = addressInfo.state
                ),
                
                FormItem.DropdownField(
                    "country", 
                    "Country *", 
                    "Select country", 
                    COUNTRIES, 
                    selectedValue = selectedCountry
                ),

                FormItem.SectionHeader("RESTAURANT CONTACT", sectionNumber = "2"),
                FormItem.PhoneInputField(
                    "contact_number", 
                    "Contact number *", 
                    "00000 00000", 
                    COUNTRY_CODES,
                    selectedCode = contactCode,
                    phoneNumber = contactNum
                ),
                FormItem.InputField(
                    "contact_email", 
                    "Contact email *", 
                    "e.g. business@gmail.com", 
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 
                    value = addressInfo.contactEmail
                ),
                FormItem.PhoneInputField(
                    "whatsapp", 
                    "WhatsApp", 
                    "00000 00000", 
                    COUNTRY_CODES,
                    selectedCode = whatsappCode,
                    phoneNumber = whatsappNum
                ),
                FormItem.InputField(
                    "website", 
                    "Website", 
                    "https://www.your-restaurant.com", 
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI, 
                    value = addressInfo.website
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 2 fields", e)
        }
    }

    private fun splitPhone(fullNumber: String, defaultCode: String): Pair<String, String> {
        val cleanDigits = fullNumber.replace("[^0-9]".toRegex(), "")
        if (cleanDigits.isEmpty()) return Pair(defaultCode, "")

        val subscriberNum = if (cleanDigits.length >= 10) cleanDigits.takeLast(10) else cleanDigits
        val countryDigits = if (cleanDigits.length > 10) cleanDigits.dropLast(10) else ""

        val matchedCode = when (countryDigits) {
            "91" -> "+91 (IN)"
            "1" -> "+1 (US)"
            "44" -> "+44 (UK)"
            "971" -> "+971 (AE)"
            "61" -> "+61 (AU)"
            "65" -> "+65 (SG)"
            "49" -> "+49 (DE)"
            else -> defaultCode
        }

        return Pair(matchedCode, subscriberNum)
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
                when (item) {
                    is FormItem.InputField -> {
                        val error = when (item.key) {
                            "address" -> FormValidator.validateNotEmpty(item.value, "Address")
                            "pin_code" -> FormValidator.validatePinCode(item.value)
                            "city" -> FormValidator.validateNotEmpty(item.value, "City")
                            "contact_email" -> FormValidator.validateEmail(item.value)
                            else -> null
                        }

                        if (error != null) {
                            isValid = false
                            currentFields[index] = item.copy(error = error)
                        } else {
                            currentFields[index] = item.copy(error = null)
                        }
                    }
                    is FormItem.DropdownField -> {
                        val error = when (item.key) {
                            "state" -> FormValidator.validateNotEmpty(item.selectedValue, "State")
                            "country" -> FormValidator.validateNotEmpty(item.selectedValue, "Country")
                            else -> null
                        }

                        if (error != null) {
                            isValid = false
                            currentFields[index] = item.copy(error = error)
                        } else {
                            currentFields[index] = item.copy(error = null)
                        }
                    }
                    is FormItem.PhoneInputField -> {
                        val error = if (item.key == "contact_number") {
                            FormValidator.validatePhone(item.phoneNumber)
                        } else null

                        if (error != null) {
                            isValid = false
                            currentFields[index] = item.copy(error = error)
                        } else {
                            currentFields[index] = item.copy(error = null)
                        }
                    }
                    else -> {}
                }
            }

            if (isValid) {
                Log.i(TAG, "Step 2 validation successful.")
                _uiState.value = RegistrationUiState.Success("Step 2 Validated", "")
            } else {
                Log.e(TAG, "Step 2 validation failed.")
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
