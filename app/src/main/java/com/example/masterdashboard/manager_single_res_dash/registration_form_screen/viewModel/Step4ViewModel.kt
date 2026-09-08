package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.FormItem
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.uiState.RegistrationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Step4ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step4ViewModel"
    }

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    fun initFields(data: RegistrationDataModel) {
        try {
            Log.d(TAG, "Initializing fields for Step 4 (Branding & Operations).")
            _formFields.value = listOf(
                FormItem.StepProgress("STEP 4 OF 5", "Branding & Operations", "Logo, schedule and operating preferences."),
                
                FormItem.UploadField(
                    "logo", 
                    "RESTAURANT LOGO", 
                    "PNG or JPG - up to 2 MB - resized automatically.",
                    imageUri = data.restaurantLogoUri
                ),
                
                FormItem.SwitchField(
                    "show_logo", 
                    "Show logo on paper receipts", 
                    "Prints your logo at the top of thermal bills.",
                    isChecked = data.showLogoOnReceipts
                ),

                FormItem.SectionHeader("OPERATING HOURS & SEATING", sectionNumber = "1"),
                FormItem.InputField("seating", "Seating capacity", "e.g. 40", android.text.InputType.TYPE_CLASS_NUMBER, value = data.seatingCapacity),
                FormItem.InputField("open_days", "Open days / week", "7 / 7", android.text.InputType.TYPE_CLASS_TEXT, value = data.openDays),
                FormItem.DropdownField(
                    "timezone", 
                    "Timezone", 
                    "Select timezone", 
                    listOf("Asia/Kolkata", "Asia/Dubai", "America/New_York"),
                    selectedValue = data.timezone
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 4 fields", e)
        }
    }

    fun validate(): Boolean {
        Log.i(TAG, "Step 4 validation passed.")
        _uiState.value = RegistrationUiState.Success("Step 4 Validated", "")
        return true
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
