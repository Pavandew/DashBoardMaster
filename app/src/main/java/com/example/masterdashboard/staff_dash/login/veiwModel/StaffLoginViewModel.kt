package com.example.masterdashboard.staff_dash.login.veiwModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.staff_dash.login.repo.StaffLoginRepository
import com.example.masterdashboard.staff_dash.login.uistate.StaffLoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffLoginViewModel(
    private val repository: StaffLoginRepository = StaffLoginRepository()
) : ViewModel(){

    companion object {
        private const val TAG = "StaffLoginViewModel"
    }

    private val _loginState = MutableStateFlow<StaffLoginUiState>(StaffLoginUiState.Idle)
    val loginState: StateFlow<StaffLoginUiState> = _loginState.asStateFlow()

    fun processStaffLogin(staffId: String, passwordStr: String) {

        // ✅ FIXED: Only trim whitespace spacing. Do NOT force uppercase mapping here.
        // This preserves the exact casing typed by the user to achieve a true literal match.
        val cleanId = staffId.trim()
        val cleanPass = passwordStr.trim()

        // 1. Core Syntax Input Validations
        if (cleanId.isEmpty()) {
            _loginState.value = StaffLoginUiState.ValidationError("Enter Staff ID")
            return
        }
        if (cleanPass.isEmpty()) {
            _loginState.value = StaffLoginUiState.ValidationError("Enter Password")
            return
        }

        _loginState.value = StaffLoginUiState.Loading
        Log.d(TAG, "ViewModel: Forwarding strict literal search request matching ID: '$cleanId' downstream to Repository layer")

        // 2. Fire Async coroutine Repository lookup Core sequence
        viewModelScope.launch {
            repository.findStaffProfileById(cleanId).fold(
                onSuccess = { (staffProfile, ownerUid) ->

                    // Account Status Verification checks
                    if (staffProfile.status.equals("Suspended", ignoreCase = true)) {
                        Log.w(TAG, "Access Denied: Account profile structure $cleanId is currently suspended.")
                        _loginState.value = StaffLoginUiState.AuthError("Access Denied: This staff account is suspended.")
                        return@launch
                    }

                    // Plain text structural verification checks matching custom password matrix
                    if(staffProfile.password == cleanPass) {
                        Log.i(TAG, "Success: Credentials match verification confirmed for user ${staffProfile.staffName}")

                        // Pass required Configuration routing tokens back to UI listener scopes
                        _loginState.value = StaffLoginUiState.Success(
                            staffName = staffProfile.staffName,
                            restaurantOwnerUid = ownerUid, // Linked root workspace parameters mapping keys (Owner UID)
                            staffDocId = staffProfile.id, // Database Auto-ID for this staff member
                            staffId = staffProfile.staffId, // Alphanumeric Custom ID (e.g. PAVAN9730)
                            permissions = staffProfile.permissions
                        )
                    } else {
                        Log.w(TAG, "Failure: Mismatched password entries submitted against key code ID: $cleanId")
                        _loginState.value = StaffLoginUiState.AuthError("Invalid Password PIN. Please try again.")
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Database Sync Failure during account verification pipelines initialization", exception)
                    _loginState.value = StaffLoginUiState.AuthError(exception.message ?: "Invalid Staff ID or Profile mismatch.")
                }
            )
        }
    }

    fun resetStateToIdle() {
        _loginState.value = StaffLoginUiState.Idle
    }
}