package com.example.masterdashboard.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.login.uistate.SignUpUiState
import com.example.masterdashboard.utils.SignUpValidator
import com.example.masterdashboard.utils.ValidationResult
import com.example.masterdashboard.utils.AppConstants
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel : ViewModel() {

    private val db = Firebase.firestore
    companion object{
        const val TAG = "SignUpViewModel"
    }

    private val _signUpState =
        MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)

    val signUpState: StateFlow<SignUpUiState> = _signUpState

    fun signUpUser(
        fullName: String,
        phone: String,
        password: String,
        confirmPassword: String,
        role: String,
        portalType: String
    ) {
        Log.i(TAG, "signUpUser: Attempting to sign up user with phone: $phone")

        // validation
        val result = SignUpValidator.validate(
            fullName,
            phone,
            password,
            confirmPassword
        )

        when (result) {

            is ValidationResult.Error -> {
                Log.w(TAG, "signUpUser: Validation failed for field: ${result.field} - ${result.message}")
                _signUpState.value =
                    SignUpUiState.Error(
                        result.field,
                        result.message
                    )
                return
            }

            is ValidationResult.Success -> {
                Log.d(TAG, "signUpUser: Validation successful, checking if user already exists")

                viewModelScope.launch {

                    _signUpState.value =
                        SignUpUiState.Loading

                    try {
                        val formattedPhone = if (phone.startsWith("+91")) phone else "+91$phone"
                        Log.d(TAG, "signUpUser: Checking existing user with formatted phone: $formattedPhone")

                        // check Existing User - check both 'mobile' and 'phone' fields
                        var snapshot = db.collection(AppConstants.COLLECTION_USERS)
                            .whereEqualTo(AppConstants.FIELD_MOBILE, formattedPhone)
                            .get()
                            .await()

                        if (snapshot.isEmpty) {
                            snapshot = db.collection(AppConstants.COLLECTION_USERS)
                                .whereEqualTo("phone", formattedPhone)
                                .get()
                                .await()
                        }

                        if (!snapshot.isEmpty) {
                            Log.w(TAG, "signUpUser: Phone/Mobile $formattedPhone is already registered")

                            _signUpState.value =
                                SignUpUiState.Error(
                                    AppConstants.FIELD_MOBILE,
                                    "Mobile already registered"
                                )
                            return@launch
                        }

                        // Success -> Send OTP
                        Log.i(TAG, "signUpUser: User not found, sending OTP to $phone")
                        _signUpState.value =
                            SignUpUiState.Success(
                                message = "OTP Sent",
                                phone = phone,
                                role = role,
                                portalType = portalType
                            )

                    } catch (e: Exception) {
                        Log.e(TAG, "signUpUser: Firestore error", e)

                        _signUpState.value =
                            SignUpUiState.Error(
                                "",
                                e.message ?: "Something went wrong"
                            )
                    }
                }
            }
        }
    }

    fun resetState() {
        _signUpState.value = SignUpUiState.Idle
    }
}