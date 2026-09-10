package com.example.masterdashboard.manager_single_res_dash.registration_form_screen.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.FormItem
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.registration_form_screen.uiState.RegistrationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Step5ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step5ViewModel"
    }

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    fun initReviewData(data: RegistrationDataModel, isEditMode: Boolean = false, onEditStep: (Int) -> Unit) {
        try {
            Log.i(TAG, "Step 5: Compiling summary (isEditMode: $isEditMode)...")

            val progressStep = if (isEditMode) "REVIEW CHANGES" else "STEP 5 OF 5"
            val progressTitle = if (isEditMode) "Update Business Profile" else "Review & Launch"
            val progressSub = if (isEditMode) "Review changes before saving back to Settings." else "Verify everything, then go live."

            _formFields.value = listOf(
                FormItem.StepProgress(progressStep, progressTitle, progressSub),

                FormItem.ReviewHeader(
                    name = data.restaurantName,
                    type = data.businessType
                ),

                FormItem.ReviewCard(
                    title = "OWNER & RESTAURANT",
                    details = listOf(
                        "Owner" to "${data.ownerFullName} (${data.ownerMobile})",
                        "Restaurant" to data.restaurantName,
                        "Type" to data.businessType
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Owner/Restaurant clicked")
                        onEditStep(1)
                    }
                ),

                FormItem.ReviewCard(
                    title = "LOCATION & CONTACT",
                    details = listOf(
                        "Address" to "${data.address}, ${data.city}, ${data.state}, ${data.pinCode}",
                        "Contact" to data.contactNumber
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Location clicked")
                        onEditStep(2)
                    }
                ),

                FormItem.ReviewCard(
                    title = "TAX & COMPLIANCE",
                    details = listOf(
                        "GST" to data.gstNumber.ifEmpty { "Not Added" },
                        "FSSAI" to data.fssaiNumber.ifEmpty { "Not Added" },
                        "Tax" to "${data.defaultTaxRate}% (${if (data.chargeTaxOnBills) "added" else "none"})"
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Tax clicked")
                        onEditStep(3)
                    }
                ),

                FormItem.ReviewCard(
                    title = "BRANDING & OPERATIONS",
                    details = listOf(
                        "Logo" to if (data.restaurantLogoUri != null) "Uploaded" else "Default",
                        "Seating" to data.seatingCapacity,
                        "Open days" to data.openDays
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Operations clicked")
                        onEditStep(4)
                    }
                ),

                FormItem.InfoCard("Bank details, UPI QR, receipt format and thermal printer settings can be customized anytime from Settings.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Step 5 review data", e)
        }
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
