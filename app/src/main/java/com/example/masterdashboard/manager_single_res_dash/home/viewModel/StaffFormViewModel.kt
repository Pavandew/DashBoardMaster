package com.example.masterdashboard.manager_single_res_dash.home.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.manager_single_res_dash.home.uistate.FirebaseUiState
import com.example.masterdashboard.manager_single_res_dash.home.models.StaffDataModel
import com.example.masterdashboard.manager_single_res_dash.home.repo.StaffFormRepository
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

    // Hold the temporary form data across fragments
    private val _currentStaffData = MutableStateFlow(StaffDataModel())
    val currentStaffData: StateFlow<StaffDataModel> = _currentStaffData

    // Hold the firestore submission status
    private val _uiState = MutableStateFlow<FirebaseUiState>(FirebaseUiState.Idle)
    val uiState: StateFlow<FirebaseUiState> = _uiState

    // check phone registered or not
    private val _phoneState = MutableStateFlow<PhoneCheckState>(PhoneCheckState.Idle)
    val phoneState: StateFlow<PhoneCheckState> = _phoneState.asStateFlow()
    // Step 1 calls this to save details before navigating
    fun updateStep1Data(
        staffName: String, mobile: String, email: String, gender: String,
        role: String, department: String, joiningData: String, shift: String, salary: String
    ) {
        // Single log for Step 1 cache data
        Log.d(TAG, "➡STEP 1 CACHED: [Name=$staffName, Mobile=$mobile, Email=$email, Gender=$gender, Role=$role, Dept=$department, JoinDate=$joiningData, Shift=$shift, Salary=$salary]")

        _currentStaffData.value = _currentStaffData.value.copy(
            staffName = staffName, mobile = mobile, email = email, gender = gender,
            role = role, department = department, joiningDate = joiningData, shift = shift, salary = salary
        )
    }

    // Step 2 calls this to append document data and submit directly to firebase
    fun submitFinalStaffData(
        ownerUid: String, documentType: String, documentNumber: String, permission: List<String>
    ) {
        val finalData = _currentStaffData.value.copy(
            documentType = documentType,
            documentNumber = documentNumber,
            permissions = permission
        )

        // ... (logging omitted for brevity in targetContent, but I should keep it)
        // Actually I should include the whole function to be safe or just the signature and call.


        // Single formatted log block showing the entire payload going to Firebase
        Log.d(TAG, "FIREBASE PAYLOAD: {\n" +
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
                "  \"permissions\": ${finalData.permissions.associateWith { true }}\n" +
                "}")

        viewModelScope.launch {
            _uiState.value = FirebaseUiState.Loading

            val result = repo.saveStaffToFirestore(finalData, ownerUid)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Firebase Upload Success!")
                    _uiState.value = FirebaseUiState.Success
                    // Optionally reset form data after successful submission
                    _currentStaffData.value = StaffDataModel()
                },
                onFailure = {
                    Log.e(TAG, " Firebase Upload Failed: ${it.message}", it)
                    _uiState.value = FirebaseUiState.Error(it.message ?: "Submission failed")
                }
            )
        }
    }

    fun verifyMobileAndProceed(
        ownerUid: String,
        mobile: String,
        onSuccessToProceed: () -> Unit
    ) {
        viewModelScope.launch {
            _phoneState.value = PhoneCheckState.Checking

            repo.isMobileNumberRegistered(ownerUid, mobile).fold(
                onSuccess = { isRegistered ->
                    if (isRegistered) {
                        Log.d(TAG, "Phone number is already registered: $mobile")
                        _phoneState.value = PhoneCheckState.AlreadyRegistered
                    } else {
                        _phoneState.value = PhoneCheckState.Available
                        // Validation passes completely -> trigger the navigation lambda block directly
                        onSuccessToProceed()
                    }
                },
                onFailure = {
                    _phoneState.value = PhoneCheckState.Error(it.message ?: "Verification failed")
                }
            )
        }
    }

    fun resetPhoneState() {
        _phoneState.value = PhoneCheckState.Idle
    }

    fun resetState() {
        _uiState.value = FirebaseUiState.Idle
    }
}