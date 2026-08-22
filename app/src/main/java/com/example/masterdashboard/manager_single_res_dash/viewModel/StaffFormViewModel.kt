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

    // ✅ UPDATED: Removed auto-generated credentials from Step 1
    fun updateStep1Data(
        staffName: String, mobile: String, email: String, gender: String,
        role: String, department: String, joiningData: String, shift: String, salary: String
    ) {
        Log.d(TAG, "➡ STEP 1 CACHED: [Name=$staffName, Mobile=$mobile, Role=$role]")

        _currentStaffData.value = _currentStaffData.value.copy(
            staffName = staffName, mobile = mobile, email = email, gender = gender,
            role = role, department = department, joiningDate = joiningData, shift = shift, salary = salary
        )
    }

    fun submitFinalStaffData(
        ownerUid: String, documentType: String, documentNumber: String, permission: List<String>
    ) {
        // ✅ GENERATE CREDENTIALS AT THE LAST MOMENT
        val name = _currentStaffData.value.staffName
        val mobile = _currentStaffData.value.mobile

        val cleanedName = name.replace("\\s".toRegex(), "").uppercase()
        val mobileSuffix = if (mobile.length >= 4) mobile.substring(mobile.length - 4) else mobile
        val generatedStaffId = "${cleanedName}${mobileSuffix}"
        val generatedPassword = generateRandomPin()

        // Appends step 2 validation criteria and newly generated credentials
        val finalData = _currentStaffData.value.copy(
            staffId = generatedStaffId,
            password = generatedPassword,
            documentType = documentType,
            documentNumber = documentNumber,
            permissions = permission
        )

        Log.d(TAG, "FINAL SUBMISSION PAYLOAD: ID=$generatedStaffId, Pass=$generatedPassword")

        viewModelScope.launch {
            _uiState.value = FirebaseUiState.Loading

            val result = repo.saveStaffToFirestore(finalData, ownerUid)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Firebase Upload Success!")
                    // We keep the final data in _currentStaffData temporarily so UI can show the popup
                    _currentStaffData.value = finalData
                    _uiState.value = FirebaseUiState.Success
                },
                onFailure = {
                    Log.e(TAG, " Firebase Upload Failed: ${it.message}", it)
                    _uiState.value = FirebaseUiState.Error(it.message ?: "Submission failed")
                }
            )
        }
    }

    private fun generateRandomPin(): String {
        val numbersList = "1234567890"
        return (1..6).map { numbersList.random() }.joinToString("")
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

