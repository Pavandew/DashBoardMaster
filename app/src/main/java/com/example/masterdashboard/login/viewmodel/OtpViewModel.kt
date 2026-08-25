package com.example.masterdashboard.login.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.login.models.UserModelData
import com.example.masterdashboard.login.repo.AuthRepository
import com.example.masterdashboard.login.uistate.OtpUiState
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OtpViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val TAG = "OtpViewModel"

    private val _otpState =
        MutableStateFlow<OtpUiState>(OtpUiState.Idle)

    val otpState: StateFlow<OtpUiState> = _otpState

    private var storedVerificationId: String? = null

    private var tempPhone: String = ""
    private var tempFullName: String = ""
    private var tempPassword: String = ""
    private var tempRole: String = ""
    private var tempPortal: String = ""

    // STEP 1: SEND OTP
    fun sendOtp(
        phone: String,
        activity: Activity,
        fullName: String,
        password: String,
        role: String,
        portalType: String
    ) {
        Log.i(TAG, "sendOtp: Initiating OTP request for phone: $phone")

        tempPhone = phone
        tempFullName = fullName
        tempPassword = password
        tempRole = role
        tempPortal = portalType

        _otpState.value = OtpUiState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.i(TAG, "onVerificationCompleted: Auto-verification successful")
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                _otpState.value = OtpUiState.Error(e.message ?: "Verification failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "onCodeSent: Verification ID received: $verificationId")
                storedVerificationId = verificationId
                _otpState.value = OtpUiState.CodeSent
            }
        }

        repository.sendOtp(tempPhone, activity, callbacks)
    }

    // STEP 2: VERIFY OTP
    fun verifyOtp(code: String) {
        Log.i(TAG, "verifyOtp: Manually verifying code")

        val verificationId = storedVerificationId

        if (verificationId == null) {
            _otpState.value = OtpUiState.Error("OTP not sent yet")
            return
        }

        _otpState.value = OtpUiState.Loading

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    // STEP 3: FIREBASE AUTH + FIRESTORE SAVE
    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            repository.signInWithCredential(credential).fold(
                onSuccess = { uid ->
                    val user = UserModelData(
                        uid = uid,
                        fullName = tempFullName,
                        mobile = if (tempPhone.startsWith("+91")) tempPhone else "+91$tempPhone",
                        passwordHash = repository.hashPassword(tempPassword),
                        role = tempRole,
                        portalType = tempPortal,
                        isVerified = true,
                        status = "Active"
                    )

                    repository.saveUserProfile(user).fold(
                        onSuccess = {
                            _otpState.value = OtpUiState.Verified(uid)
                        },
                        onFailure = { e ->
                            _otpState.value = OtpUiState.Error("Failed to create profile: ${e.message}")
                        }
                    )
                },
                onFailure = { e ->
                    _otpState.value = OtpUiState.Error("Wrong OTP or sign-in failed")
                }
            )
        }
    }
}
