package com.example.masterdashboard.manager_single_res_dash.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.uistate.FirebaseUiState
import com.example.masterdashboard.manager_single_res_dash.models.StaffDataModel
import com.example.masterdashboard.manager_single_res_dash.repo.StaffFormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhoneCheckState {
    object Idle : PhoneCheckState
    object Checking : PhoneCheckState
    object Available : PhoneCheckState
    object AlreadyRegistered : PhoneCheckState
    data class Error(val errorMsg: String) : PhoneCheckState
}

class StaffFormViewModel(
    private val repo: StaffFormRepository = StaffFormRepository()
): ViewModel() {

    companion object {
        private const val TAG = "StaffFormViewModel"
    }

    private val _currentStaffData = MutableStateFlow(StaffDataModel())
    val currentStaffData: StateFlow<StaffDataModel> = _currentStaffData

    private val _uiState = MutableStateFlow<FirebaseUiState>(FirebaseUiState.Idle)
    val uiState: StateFlow<FirebaseUiState> = _uiState

    private val _phoneState = MutableStateFlow<PhoneCheckState>(PhoneCheckState.Idle)
    val phoneState: StateFlow<PhoneCheckState> = _phoneState.asStateFlow()

    fun updateStaffData(data: StaffDataModel) {
        _currentStaffData.value = data
    }

    // ✅ UPDATED: Accepts auto-generated credentials strings payload parameters from Step 1 Fragment
    fun updateStep1Data(
        staffName: String, mobile: String, email: String, gender: String,
        role: String, department: String, joiningData: String, shift: String, salary: String,
        staffId: String, password: String
    ) {
        Log.d(TAG, "➡ STEP 1 CACHED: [Name=$staffName, ID=$staffId, Pass=$password, Mobile=$mobile, Role=$role]")

        _currentStaffData.value = _currentStaffData.value.copy(
            staffName = staffName, mobile = mobile, email = email, gender = gender,
            role = role, department = department, joiningDate = joiningData, shift = shift, salary = salary,
            staffId = staffId,     // ✅ Saved to shared flow memory block layout references
            password = password   // ✅ Saved to shared flow memory block layout references
        )
    }

    fun submitFinalStaffData(
        ownerUid: String, documentType: String, documentNumber: String, permission: List<String>
    ) {
        // Appends step 2 validation criteria selections cleanly over top of step 1 cache boundaries
        val finalData = _currentStaffData.value.copy(
            documentType = documentType,
            documentNumber = documentNumber,
            permissions = permission
        )

        Log.d(TAG, "FINAL SUBMISSION PAYLOAD INCLUDING ID & PASS: {\n" +
                "  \"staffId\": \"${finalData.staffId}\",\n" +
                "  \"password\": \"${finalData.password}\",\n" +
                "  \"staffName\": \"${finalData.staffName}\",\n" +
                "  \"mobile\": \"${finalData.mobile}\",\n" +
                "  \"email\": \"${finalData.email}\",\n" +
                "  \"gender\": \"${finalData.gender}\",\n" +
                "  \"role\": \"${finalData.role}\",\n" +
                "  \"department\": \"${finalData.department}\",\n" +
                "  \"joiningDate\": \"${finalData.joiningDate}\",\n" +
                "  \"shift\": \"${finalData.shift}\",\n" +
                "  \"salary\": \"${finalData.salary}\",\n" +
                "  \"documentType\": \"${finalData.documentType}\",\n" +
                "  \"documentNumber\": \"${finalData.documentNumber}\",\n" +
                "  \"permissions\": ${finalData.permissions}\n" +
                "}")

        viewModelScope.launch {
            _uiState.value = FirebaseUiState.Loading

            val result = repo.saveStaffToFirestore(finalData, ownerUid)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Firebase Upload Success!")
                    _uiState.value = FirebaseUiState.Success
                    _currentStaffData.value = StaffDataModel() // Clean state post-success
                },
                onFailure = {
                    Log.e(TAG, " Firebase Upload Failed: ${it.message}", it)
                    _uiState.value = FirebaseUiState.Error(it.message ?: "Submission failed")
                }
            )
        }
    }

    fun verifyMobileAndProceed(ownerUid: String, mobile: String, onSuccessToProceed: () -> Unit) {
        viewModelScope.launch {
            _phoneState.value = PhoneCheckState.Checking
            repo.isMobileNumberRegistered(ownerUid, mobile).fold(
                onSuccess = { isRegistered ->
                    if (isRegistered) {
                        Log.d(TAG, "Phone number is already registered: $mobile")
                        _phoneState.value = PhoneCheckState.AlreadyRegistered
                    } else {
                        _phoneState.value = PhoneCheckState.Available
                        onSuccessToProceed() // Triggers the navigation blocks setup instantly
                    }
                },
                onFailure = {
                    _phoneState.value = PhoneCheckState.Error(it.message ?: "Verification failed")
                }
            )
        }
    }

    fun resetPhoneState() { _phoneState.value = PhoneCheckState.Idle }
    fun resetState() { _uiState.value = FirebaseUiState.Idle }
    fun clearFormData() {
        _currentStaffData.value = StaffDataModel()
        _phoneState.value = PhoneCheckState.Idle
        _uiState.value = FirebaseUiState.Idle
        Log.d(TAG, "Staff form data cleared")
    }
}