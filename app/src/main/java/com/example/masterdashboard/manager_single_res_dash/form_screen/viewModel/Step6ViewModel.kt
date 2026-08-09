package com.example.masterdashboard.manager_single_res_dash.form_screen.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.FormItem
import com.example.masterdashboard.manager_single_res_dash.form_screen.model.RegistrationDataModel
import com.example.masterdashboard.manager_single_res_dash.form_screen.repository.RegistrationRepository
import com.example.masterdashboard.manager_single_res_dash.form_screen.uiState.RegistrationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * STEP 6 VIEWMODEL (THE "WORKER")
 * Purpose: Handles the final "Review & Launch" logic.
 * It compiles data from all previous steps into a summary view.
 */
class Step6ViewModel : ViewModel() {

    companion object {
        private const val TAG = "Step6ViewModel"
    }

    private val repository = RegistrationRepository()

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _formFields = MutableStateFlow<List<FormItem>>(emptyList())
    val formFields: StateFlow<List<FormItem>> = _formFields.asStateFlow()

    /**
     * compiles all registration data into a list of Review Cards.
     */
    fun initReviewData(data: RegistrationDataModel, onEditStep: (Int) -> Unit) {
        try {
            Log.i(TAG, "Step 6: Fetching and compiling registration summary...")

            // Log key data points to verify fetch success
            val filledData = data.toMap()
            Log.d(TAG, "Data Items Fetched: ${filledData.size}")

            _formFields.value = listOf(
                FormItem.StepProgress("STEP 6 OF 6", "Review & Launch", "Verify everything, then go live."),

                FormItem.ReviewHeader(
                    name = data.restaurantName,
                    type = data.businessType
                ),

                FormItem.ReviewCard(
                    title = "OWNER",
                    details = listOf(
                        "Name" to data.ownerFullName,
                        "Email" to data.ownerEmail,
                        "Mobile" to data.ownerMobile
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Owner clicked")
                        onEditStep(1)
                    }
                ),

                FormItem.ReviewCard(
                    title = "RESTAURANT",
                    details = listOf(
                        "Name" to data.restaurantName,
                        "Type" to data.businessType,
                        "Legal name" to data.legalName,
                        "Display name" to data.displayName
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Restaurant clicked")
                        onEditStep(1)
                    }
                ),

                FormItem.ReviewCard(
                    title = "LOCATION",
                    details = listOf(
                        "Address" to "${data.address}, ${data.city}, ${data.state}, ${data.pinCode}, ${data.country}",
                        "Contact" to data.contactNumber
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Location clicked")
                        onEditStep(2)
                    }
                ),

                FormItem.ReviewCard(
                    title = "OPERATIONS",
                    details = listOf(
                        "Seating" to data.seatingCapacity,
                        "Open days" to data.openDays,
                        "Timezone" to data.timezone
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Operations clicked")
                    } // Future step
                ),

                FormItem.ReviewCard(
                    title = "TAX",
                    details = listOf(
                        "GST" to data.gstNumber,
                        "PAN" to data.panNumber,
                        "FSSAI" to data.fssaiNumber,
                        "Tax" to "${data.defaultTaxRate}% (${if (data.chargeTaxOnBills) "added" else "none"})"
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Tax clicked")
                        onEditStep(3)
                    }
                ),

                FormItem.ReviewCard(
                    title = "BILLING",
                    details = listOf(
                        "Currency" to data.currency,
                        "Invoice prefix" to data.invoicePrefix,
                        "Start #" to data.startingInvoiceNumber,
                        "Print size" to data.printSize
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Billing clicked")
                        onEditStep(4)
                    }
                ),

                FormItem.ReviewCard(
                    title = "BRANDING",
                    details = listOf(
                        "Logo" to if (data.restaurantLogoUri != null) "Uploaded" else "Not added",
                        "Theme" to "Light"
                    ),
                    onEditClick = {
                        Log.d(TAG, "Review: Edit Branding clicked")
                        onEditStep(5)
                    }
                ),

                FormItem.InfoCard("Bank details, UPI, tagline and receipt footer can be added anytime from Settings → Restaurant profile.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error compiling review data: ${e.message}", e)
            _uiState.value = RegistrationUiState.Error("Failed to load review screen. Please try again.")
        }
    }

    /**
     * Triggers the final submission to Firebase.
     */
    fun launchRestaurant(data: RegistrationDataModel, ownerUid: String) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Action: Launching Restaurant for Owner: $ownerUid")
                _uiState.value = RegistrationUiState.Loading
                
                data.ownerUid = ownerUid
                val result = repository.saveFinalRegistration(data)
                
                result.onSuccess { restaurantId ->
                    Log.i(TAG, "Launch Successful! RestaurantID: $restaurantId")
                    _uiState.value = RegistrationUiState.Success("Restaurant Profile Created Successfully!", restaurantId)
                }.onFailure { e ->
                    Log.e(TAG, "Launch Failed: ${e.message}")
                    _uiState.value = RegistrationUiState.Error(e.message ?: "Failed to create restaurant")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical failure during restaurant launch", e)
                _uiState.value = RegistrationUiState.Error("Launch failed: ${e.localizedMessage}")
            }
        }
    }

    fun setIdle() {
        _uiState.value = RegistrationUiState.Idle
    }
}
