package com.example.masterdashboard.login.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.example.masterdashboard.login.uistate.OtpUiState
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

    private val _otpState =
        MutableStateFlow<OtpUiState>(OtpUiState.Idle)

    val otpState: StateFlow<OtpUiState> = _otpState

    private var storedVerificationId: String? = null

    private var tempPhone: String = ""
    private var tempFullName: String = ""
    private var tempPassword: String = ""

    // STEP 1: SEND OTP
    fun sendOtp(phone: String, activity: Activity, fullName: String, password: String) {

        tempPhone = phone
        tempFullName = fullName
        tempPassword = password

        _otpState.value = OtpUiState.Loading

        val callbacks =
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(
                    credential: PhoneAuthCredential
                ) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(
                    e: FirebaseException
                ) {
                    _otpState.value =
                        OtpUiState.Error(
                            e.message ?: "Verification failed"
                        )
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
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

        val verificationId = storedVerificationId

        if (verificationId == null) {
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

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val user = hashMapOf(
                        "uid" to uid,
                        "fullName" to tempFullName,
                        "phone" to "+91$tempPhone",
                        "passwordHash" to hashPassword(tempPassword),
                        "isVerified" to true,
                        "status" to "Active"
                    )

                    db.collection("users")
                        .document(uid)
                        .set(user)

                    _otpState.value =
                        OtpUiState.Verified

                } else {

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