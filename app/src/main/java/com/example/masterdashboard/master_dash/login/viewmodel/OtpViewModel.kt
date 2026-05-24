package com.example.masterdashboard.master_dash.login.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.master_dash.login.uistate.OtpUiState
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

class OtpViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "OtpViewModel"

    private val _otpState =
        MutableStateFlow<OtpUiState>(OtpUiState.Idle)

    val otpState: StateFlow<OtpUiState> = _otpState

    private var storedVerificationId: String? = null

    private var tempPhone: String = ""
    private var tempFullName: String = ""
    private var tempPassword: String = ""

    // STEP 1: SEND OTP
    fun sendOtp(phone: String, activity: Activity, fullName: String, password: String) {
        Log.i(TAG, "sendOtp: Initiating OTP request for phone: $phone")

        tempPhone = phone
        tempFullName = fullName
        tempPassword = password

        _otpState.value = OtpUiState.Loading

        val callbacks =
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(
                    credential: PhoneAuthCredential
                ) {
                    Log.i(TAG, "onVerificationCompleted: Auto-verification successful")
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(
                    e: FirebaseException
                ) {
                    Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                    _otpState.value =
                        OtpUiState.Error(
                            e.message ?: "Verification failed"
                        )
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(TAG, "onCodeSent: Verification ID received: $verificationId")
                    storedVerificationId = verificationId
                    _otpState.value = OtpUiState.CodeSent
                }
            }

        val options =
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+91$phone")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // STEP 2: VERIFY OTP
    fun verifyOtp(code: String) {
        Log.i(TAG, "verifyOtp: Manually verifying code")

        val verificationId = storedVerificationId

        if (verificationId == null) {
            Log.w(TAG, "verifyOtp: Verification ID is null, OTP might not have been sent")
            _otpState.value =
                OtpUiState.Error("OTP not sent yet")
            return
        }

        _otpState.value = OtpUiState.Loading

        val credential =
            PhoneAuthProvider.getCredential(
                verificationId,
                code
            )

        signInWithCredential(credential)
    }

    // STEP 3: FIREBASE AUTH + FIRESTORE SAVE
    private fun signInWithCredential(
        credential: PhoneAuthCredential
    ) {
        Log.d(TAG, "signInWithCredential: Attempting Firebase sign-in")

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    Log.i(TAG, "signInWithCredential: Sign-in successful")

                    val uid = auth.currentUser?.uid ?: run {
                        Log.e(TAG, "signInWithCredential: Current user or UID is null after successful sign-in")
                        return@addOnCompleteListener
                    }

                    val user = hashMapOf(
                        "uid" to uid,
                        "fullName" to tempFullName,
                        "phone" to "+91$tempPhone",
                        "passwordHash" to hashPassword(tempPassword),
                        "isVerified" to true,
                        "status" to "Active"
                    )

                    Log.d(TAG, "signInWithCredential: Saving user profile to Firestore for UID: $uid")
                    db.collection("users")
                        .document(uid)
                        .set(user)
                        .addOnSuccessListener {
                            Log.i(TAG, "signInWithCredential: User profile saved successfully")
                            _otpState.value = OtpUiState.Verified
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "signInWithCredential: Failed to save user profile", e)
                            _otpState.value = OtpUiState.Error("Failed to create profile: ${e.message}")
                        }

                } else {
                    Log.w(TAG, "signInWithCredential: Sign-in failed", task.exception)
                    _otpState.value =
                        OtpUiState.Error(
                            "Wrong OTP. Try again"
                        )
                }
            }
    }
    private fun hashPassword(password: String) : String {
        return password.hashCode().toString()
    }
}