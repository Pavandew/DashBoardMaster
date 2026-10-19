package com.restroone.app.manager_single_res_dash.registration_form_screen.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.restroone.app.manager_single_res_dash.registration_form_screen.model.FormItem
import com.restroone.app.manager_single_res_dash.registration_form_screen.model.RegistrationDataModel
import com.restroone.app.manager_single_res_dash.registration_form_screen.uiState.RegistrationUiState
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

            val effectiveOwner = data.getEffectiveOwnerProfile()
            val effectiveRestaurant = data.getEffectiveRestaurantProfile()
            val effectiveAddress = data.getEffectiveAddressInfo()
            val effectiveTax = data.getEffectiveTaxSettings()
            val effectiveBilling = data.getEffectiveBillingPrinterSettings()

            val progressStep = if (isEditMode) "REVIEW CHANGES" else "STEP 5 OF 5"
            val progressTitle = if (isEditMode) "Update Business Profile" else "Review & Launch"
            val progressSub = if (isEditMode) "Review changes before saving back to Settings." else "Verify everything, then go live."

            _formFields.value = listOf(
                FormItem.StepProgress(progressStep, progressTitle, progressSub),

                FormItem.ReviewHeader(
                    name = effectiveRestaurant.restaurantName,
                    type = effectiveRestaurant.businessType
                ),

                FormItem.ReviewCard(
                    title = "OWNER & RESTAURANT",
                    details = listOf(
                        "Owner" to "${effectiveOwner.ownerFullName} (${effectiveOwner.ownerMobile})",
                        "Email" to effectiveOwner.ownerEmail,
                        "Restaurant" to effectiveRestaurant.restaurantName,
                        "Type" to effectiveRestaurant.businessType
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Owner/Restaurant clicked")
                        onEditStep(1)
                    }
                ),

                FormItem.ReviewCard(
                    title = "LOCATION & CONTACT",
                    details = listOf(
                        "Address" to "${effectiveAddress.address}, ${effectiveAddress.city}, ${effectiveAddress.state}, ${effectiveAddress.pinCode}",
                        "Contact" to effectiveAddress.contactNumber
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Location clicked")
                        onEditStep(2)
                    }
                ),

                FormItem.ReviewCard(
                    title = "TAX & COMPLIANCE",
                    details = listOf(
                        "GST" to effectiveTax.gstNumber.ifEmpty { "Not Added" },
                        "FSSAI" to effectiveTax.fssaiNumber.ifEmpty { "Not Added" },
                        "Tax" to "${effectiveTax.defaultTaxRate}% (${if (effectiveTax.chargeTaxOnBills) "added" else "none"})"
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Tax clicked")
                        onEditStep(3)
                    }
                ),

                FormItem.ReviewCard(
                    title = "BRANDING & OPERATIONS",
                    details = listOf(
                        "Logo" to if (effectiveBilling.restaurantLogoUri != null) "Uploaded" else "Default",
                        "Seating" to effectiveRestaurant.seatingCapacity,
                        "Open days" to effectiveRestaurant.openDays
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
