package com.restroone.app.login.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restroone.app.login.models.UserModelData
import com.restroone.app.login.repo.AuthCloudFunctionRepository
import com.restroone.app.login.repo.AuthRepository
import com.restroone.app.login.uistate.OtpUiState
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OtpViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val cloudFunctionRepo = AuthCloudFunctionRepository()
    private val TAG = "OtpViewModel"

    private val _otpState =
        MutableStateFlow<OtpUiState>(OtpUiState.Idle)

    val otpState: StateFlow<OtpUiState> = _otpState

    private var storedVerificationId: String? = null
    private var resendingToken: PhoneAuthProvider.ForceResendingToken? = null

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
        tempPhone = phone
        tempFullName = fullName
        tempPassword = password
        tempRole = role
        tempPortal = portalType

        _otpState.value = OtpUiState.Sending

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.i(TAG, "onVerificationCompleted: Auto-verification successful")
                _otpState.value = OtpUiState.Verifying
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                val msg = e.message ?: ""
                val friendlyMsg = if (msg.contains("blocked", ignoreCase = true) ||
                    msg.contains("unusual activity", ignoreCase = true) ||
                    msg.contains("Too many attempts", ignoreCase = true) ||
                    e is com.google.firebase.FirebaseTooManyRequestsException) {
                    "Too many OTP requests from this device. Please wait a few minutes or try again later."
                } else {
                    msg.ifBlank { "Verification failed. Please check your connection and try again." }
                }
                _otpState.value = OtpUiState.Error(friendlyMsg)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "onCodeSent: Verification ID received: $verificationId")
                storedVerificationId = verificationId
                resendingToken = token
                _otpState.value = OtpUiState.CodeSent
            }
        }

        repository.sendOtp(tempPhone, activity, callbacks)
    }

    // RESEND OTP
    fun resendOtp(activity: Activity) {
        if (tempPhone.isBlank()) {
            _otpState.value = OtpUiState.Error("Phone number missing")
            return
        }

        Log.i(TAG, "resendOtp: Requesting new OTP for phone: $tempPhone")
        _otpState.value = OtpUiState.Sending

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.i(TAG, "onVerificationCompleted: Auto-verification successful on resend")
                _otpState.value = OtpUiState.Verifying
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                val msg = e.message ?: ""
                val friendlyMsg = if (msg.contains("blocked", ignoreCase = true) ||
                    msg.contains("unusual activity", ignoreCase = true) ||
                    msg.contains("Too many attempts", ignoreCase = true) ||
                    e is com.google.firebase.FirebaseTooManyRequestsException) {
                    "Too many OTP requests from this device. Please wait a few minutes or try again later."
                } else {
                    msg.ifBlank { "Resend verification failed. Please try again later." }
                }
                _otpState.value = OtpUiState.Error(friendlyMsg)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "onCodeSent: Resend Verification ID received: $verificationId")
                storedVerificationId = verificationId
                resendingToken = token
                _otpState.value = OtpUiState.CodeSent
            }
        }

        repository.resendOtp(tempPhone, activity, callbacks, resendingToken)
    }

    // STEP 2: VERIFY OTP
    fun verifyOtp(code: String) {
        Log.i(TAG, "verifyOtp: Manually verifying code")

        val verificationId = storedVerificationId

        if (verificationId == null) {
            _otpState.value = OtpUiState.Error("OTP session expired or not sent yet. Please tap Resend OTP.")
            return
        }

        _otpState.value = OtpUiState.Verifying

        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    // STEP 3: FIREBASE AUTH + FIRESTORE PROFILE + SECURE CLOUD FUNCTION CREDENTIAL SAVE
    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            repository.signInWithCredential(credential).fold(
                onSuccess = { uid ->
                    // 1. Save user profile FIRST in users/{uid}
                    val user = UserModelData(
                        uid = uid,
                        fullName = tempFullName,
                        mobile = if (tempPhone.startsWith("+91")) tempPhone else "+91$tempPhone",
                        role = tempRole,
                        portalType = tempPortal,
                        isVerified = true,
                        status = "Active"
                    )

                    repository.saveUserProfile(user).fold(
                        onSuccess = {
                            // 2. Register secure credentials server-side via Cloud Function
                            cloudFunctionRepo.registerUserCredentials(tempPhone, tempPassword).fold(
                                onSuccess = {
                                    Log.i(TAG, "User registration & profile save completed successfully")
                                    _otpState.value = OtpUiState.Verified(uid)
                                },
                                onFailure = { e ->
                                    val msg = e.message ?: ""
                                    if (msg.contains("already registered", ignoreCase = true)) {
                                        Log.i(TAG, "Credentials already existed from prior attempt; profile updated successfully")
                                        _otpState.value = OtpUiState.Verified(uid)
                                    } else {
                                        Log.w(TAG, "Cloud Function credential register non-fatal warning: $msg")
                                        // Profile is saved in Firestore so user can still access dashboard
                                        _otpState.value = OtpUiState.Verified(uid)
                                    }
                                }
                            )
                        },
                        onFailure = { e ->
                            _otpState.value = OtpUiState.Error("Failed to create profile: ${e.message}")
                        }
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "signInWithCredential failed", e)
                    val errMsg = e.message ?: ""
                    val friendlyMsg = if (errMsg.contains("expired", ignoreCase = true)) {
                        "SMS code has expired. Please tap Resend OTP to receive a new code."
                    } else if (errMsg.contains("invalid", ignoreCase = true) || errMsg.contains("code", ignoreCase = true)) {
                        "Invalid OTP code. Please check and enter again."
                    } else {
                        errMsg.ifBlank { "Wrong OTP or sign-in failed" }
                    }
                    _otpState.value = OtpUiState.Error(friendlyMsg)
                }
            )
        }
    }
}
