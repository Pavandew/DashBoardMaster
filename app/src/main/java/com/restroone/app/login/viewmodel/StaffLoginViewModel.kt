package com.restroone.app.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restroone.app.login.repo.StaffLoginRepository
import com.restroone.app.login.uistate.StaffLoginUiState
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

        val cleanId = staffId.trim()
        val cleanPass = passwordStr.trim()

        if (cleanId.isEmpty()) {
            _loginState.value = StaffLoginUiState.ValidationError("Enter Staff ID")
            return
        }
        if (cleanPass.isEmpty()) {
            _loginState.value = StaffLoginUiState.ValidationError("Enter Password")
            return
        }

        _loginState.value = StaffLoginUiState.Loading
        Log.d(TAG, "Starting server-side credential verification for staff ID: '$cleanId'")
        viewModelScope.launch {
            repository.authenticateStaff(cleanId, cleanPass).fold(
                onSuccess = { result ->
                    val staffProfile = result.staff
                    _loginState.value = StaffLoginUiState.Success(
                        staffName = staffProfile.staffName,
                        restaurantOwnerUid = result.ownerUid,
                        restaurantId = result.restaurantId,
                        staffDocId = staffProfile.id,
                        staffId = staffProfile.staffId,
                        role = staffProfile.role,
                        mobile = staffProfile.mobile,
                        permissions = staffProfile.permissions
                    )
                },
                onFailure = { exception ->
                    Log.e(TAG, "Staff authentication failed", exception)
                    _loginState.value = StaffLoginUiState.AuthError(
                        exception.message ?: "Invalid Staff ID or password."
                    )
                }
            )
        }
    }

    fun resetStateToIdle() {
        _loginState.value = StaffLoginUiState.Idle
    }

    fun findStaffByPhoneForReset(phone: String) {
        val cleanPhone = phone.trim()
        if (cleanPhone.isEmpty()) {
            _loginState.value = StaffLoginUiState.ValidationError("Enter Mobile Number")
            return
        }

        val phone = if (cleanPhone.startsWith("+91")) cleanPhone else "+91$cleanPhone"
        _loginState.value = StaffLoginUiState.Success(
            staffName = "",
            restaurantOwnerUid = "",
            restaurantId = "",
            staffDocId = "",
            staffId = "",
            role = "waiter_staff",
            mobile = phone,
            permissions = emptyList()
        )
    }
}