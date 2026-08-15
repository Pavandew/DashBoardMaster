package com.example.masterdashboard.login.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.login.repo.ChangePasswordRepository
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class ChangePasswordState {
    object Idle : ChangePasswordState()
    object Loading : ChangePasswordState()
    object OtpSent : ChangePasswordState()
    object OtpVerified : ChangePasswordState()
    object Success : ChangePasswordState()
    data class Error(val message: String) : ChangePasswordState()
}

class ChangePasswordViewModel(private val repository: ChangePasswordRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ChangePasswordVM"

    private val _uiState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val uiState: StateFlow<ChangePasswordState> = _uiState

    private var storedVerificationId: String? = null

    fun sendOtp(phone: String, activity: Activity) {
        val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
        Log.i(TAG, "sendOtp: Requesting OTP for $formattedPhone")
        _uiState.value = ChangePasswordState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: Auto-verification successful")
                verifyOtpWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: OTP request failed", e)
                _uiState.value = ChangePasswordState.Error(e.message ?: "OTP Verification Failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "onCodeSent: OTP code sent. VerificationId: $verificationId")
                storedVerificationId = verificationId
                _uiState.value = ChangePasswordState.OtpSent
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        Log.i(TAG, "verifyOtp: Manually verifying code: $code")
        val verificationId = storedVerificationId ?: run {
            Log.w(TAG, "verifyOtp: verificationId is null")
            return
        }
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        verifyOtpWithCredential(credential)
    }

    private fun verifyOtpWithCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "verifyOtpWithCredential: Initiating sign-in with credential")
        _uiState.value = ChangePasswordState.Loading
        viewModelScope.launch {
            repository.signInWithCredential(credential).fold(
                onSuccess = {
                    Log.i(TAG, "verifyOtpWithCredential: OTP Verified successfully")
                    _uiState.value = ChangePasswordState.OtpVerified
                },
                onFailure = {
                    Log.e(TAG, "verifyOtpWithCredential: OTP verification failed", it)
                    _uiState.value = ChangePasswordState.Error(it.message ?: "Invalid OTP")
                }
            )
        }
    }

    fun updatePassword(
        newPassword: String,
        role: String,
        uid: String,
        staffDocId: String? = null
    ) {
        Log.i(TAG, "updatePassword: Initiating password update. Role: $role, UID: $uid")
        _uiState.value = ChangePasswordState.Loading
        viewModelScope.launch {
            val result = if (role == AppConstants.ROLE_MANAGER || 
                role == AppConstants.ROLE_OWNER_SINGLE || 
                role == AppConstants.ROLE_OWNER_MULTI) {
                Log.d(TAG, "updatePassword: Treating as Owner/Manager update")
                repository.updateOwnerPassword(uid, newPassword)
            } else {
                Log.d(TAG, "updatePassword: Treating as Staff update. StaffDocId: $staffDocId")
                if (staffDocId.isNullOrEmpty()) {
                    Log.e(TAG, "updatePassword: staffDocId is missing for non-owner role")
                    _uiState.value = ChangePasswordState.Error("Staff account error: Document ID missing")
                    return@launch
                }
                repository.updateStaffPassword(uid, staffDocId, newPassword)
            }

            result.fold(
                onSuccess = {
                    Log.i(TAG, "updatePassword: Password successfully updated in database")
                    _uiState.value = ChangePasswordState.Success
                },
                onFailure = {
                    Log.e(TAG, "updatePassword: Database update failed", it)
                    _uiState.value = ChangePasswordState.Error(it.message ?: "Failed to update password")
                }
            )
        }
    }
}

class ChangePasswordViewModelFactory(private val repository: ChangePasswordRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChangePasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChangePasswordViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
