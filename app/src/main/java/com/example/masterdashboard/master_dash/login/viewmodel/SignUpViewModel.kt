package com.example.masterdashboard.master_dash.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masterdashboard.master_dash.login.uistate.SignUpUiState
import com.example.masterdashboard.master_dash.login.utils.SignUpValidator
import com.example.masterdashboard.master_dash.login.utils.ValidationResult
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val TAG = "SignUpViewModel"

    private val _signUpState =
        MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)

    val signUpState: StateFlow<SignUpUiState> = _signUpState

    fun signUpUser(
        fullName: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        Log.i(TAG, "signUpUser: Attempting to sign up user with phone: $phone")

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

                        val snapshot = db.collection("users")
                            .whereEqualTo("phone", phone)
                            .get()
                            .await()

                        if (!snapshot.isEmpty) {
                            Log.w(TAG, "signUpUser: Phone $phone is already registered")

                            _signUpState.value =
                                SignUpUiState.Error(
                                    "phone",
                                    "Phone already registered"
                                )
                            return@launch
                        }

                        Log.i(TAG, "signUpUser: User not found, sending OTP to $phone")
                        _signUpState.value =
                            SignUpUiState.Success(
                                message = "OTP Sent",
                                phone = phone
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